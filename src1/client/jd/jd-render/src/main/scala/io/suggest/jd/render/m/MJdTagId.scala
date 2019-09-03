package io.suggest.jd.render.m

import io.suggest.ad.blk.MBlockExpandMode
import io.suggest.common.html.HtmlConstants
import io.suggest.jd.MJdDoc
import io.suggest.jd.tags.JdTag
import io.suggest.scalaz.NodePath_t
import japgolly.univeq._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import scalaz.Tree

import scala.collection.immutable.HashMap


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.03.18 22:06
  * Description: Возникла необходимость получения стабильных id для каждого блока и тега плитки,
  * чтобы убрать автоматическую адресацию ScalaCSS, которая неэффективна в плитке, вызывая пере-рендеры на каждый чих.
  *
  * Эта модель нужна для связывания css-стиля с вёрсткой через идентификатор.
  * Если какой-то блок повторяется в плитке, то проблемы в этом нет - id не является обязательно уникальным,
  * ибо тут связывающий id, ключ для доступа к стилю, и не более.
  *
  * Это нужно для оптимизации jd-рендера, чтобы убрать пере-рендеры из-за нарушения порядка стилей в css.
  */
object MJdTagId {

  implicit def univEq: UnivEq[MJdTagId] = UnivEq.derive

  val selPathRev = GenLens[MJdTagId](_.selPathRev)
  val blockExpand = GenLens[MJdTagId](_.blockExpand)

  def mkTreeIndexSeg(jdDoc: MJdDoc): Stream[(MJdTagId, JdTag)] = {
    mkTreeIndexSeg( jdDoc.template, MJdTagId(jdDoc.nodeId) )
  }

  /** Создать сегмент для будущего jd-индекса по id.
    *
    * @param template Исходный документ.
    * @param jdTagId id текущего тега.
    * @return Максимально ленивый Stream, на основе которого можно создать индекс.
    */
  def mkTreeIndexSeg(template: Tree[JdTag], jdTagId: MJdTagId): Stream[(MJdTagId, JdTag)] = {
    // Если текущий тег - это блок, то надо сбросить значение expand на текущее значение.
    val jdt = template.rootLabel
    val expandMode2 = jdt.props1.bm .flatMap(_.expandMode)

    val jdTagId2 = if (expandMode2 !=* jdTagId.blockExpand) {
      blockExpand.set(expandMode2)( jdTagId )
    } else {
      jdTagId
    }

    // Отработать текущий элемент
    val el0 = jdTagId2 -> jdt

    el0 #:: {
      // И отработать дочерние элементы:
      template
        .subForest
        .zipWithIndex
        .flatMap { case (subJdt, i) =>
          val jdTagChild2 = selPathRev.modify(i :: _)( jdTagId2 )
          mkTreeIndexSeg( subJdt, jdTagChild2 )
        }
    }
  }


  def mkTreeIndex(segments: Stream[(MJdTagId, JdTag)]*): HashMap[MJdTagId, JdTag] =
    mkTreeIndex1( segments )
  /** Сборка кусков-сегментов в единый индекс. */
  def mkTreeIndex1(segments: TraversableOnce[Stream[(MJdTagId, JdTag)]]): HashMap[MJdTagId, JdTag] = {
    (HashMap.newBuilder[MJdTagId, JdTag] ++= segments.toStream.iterator.flatten)
      .result()
  }

}


/** Класс-контейнер данных идентификатора блока.
  *
  * @param nodeId id узла-карточки, если есть.
  * @param selPathRev Порядковый номер до тега внутри jd-документа (одной карточки).
  *                   НеЭквивалентен по смыслу к MJdRenderArgs.selPath
  * @param blockExpand Режим отображения блока.
  */
final case class MJdTagId(
                           nodeId           : Option[String]              = None,
                           selPathRev       : NodePath_t                  = Nil,
                           blockExpand      : Option[MBlockExpandMode]    = None,
                         ) {

  override val toString: String = {
    var acc: List[Any] = selPathRev
    for (bexp <- blockExpand)
      acc ::= bexp.value
    for (id <- nodeId)
      acc ::= id
    acc.mkString( HtmlConstants.MINUS )
  }

  def selPath = selPathRev.reverse

}
