package io.suggest.jd

import diode.FastEq
import io.suggest.ad.blk.MBlockExpandMode
import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.common.html.HtmlConstants
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.primo.IHashCodeLazyVal
import io.suggest.scalaz.NodePath_t
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
object MJdTagId extends IEmpty {

  override type T = MJdTagId
  override def empty = apply()

  implicit def univEq: UnivEq[MJdTagId] = UnivEq.derive

  implicit def jdTagIdJson: OFormat[MJdTagId] = (
    (__ \ "i").formatNullable[String] and
    (__ \ "p").formatNullable[NodePath_t]
      .inmap[NodePath_t](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        np => if (np.isEmpty) None else Some(np),
      ) and
    (__ \ "e").formatNullable[MBlockExpandMode]
  )(apply, unlift(unapply))


  val selPathRev = GenLens[MJdTagId](_.selPathRev)
  val blockExpand = GenLens[MJdTagId](_.blockExpand)


  def mkTreesIndexSeg(jdDocs: Stream[MJdDoc]): Stream[(MJdTagId, JdTag)] =
    jdDocs.flatMap( mkTreeIndexSeg )

  /** Создать сегмент для будущего jd-индекса по id.
    *
    * @param jdDoc Данные исходного документа.
    * @return Максимально ленивый Stream, на основе которого можно создать индекс.
    */
  def mkTreeIndexSeg(jdDoc: MJdDoc): Stream[(MJdTagId, JdTag)] = {
    // Если текущий тег - это блок, то надо сбросить значение expand на текущее значение.
    val jdt = jdDoc.template.rootLabel
    val expandMode2 = jdt.props1.bm
      .flatMap(_.expandMode)

    val jdTagId2 = if (
      (jdt.name ==* MJdTagNames.STRIP) &&
      (expandMode2 !=* jdDoc.jdId.blockExpand)
    ) {
      blockExpand.set(expandMode2)( jdDoc.jdId )
    } else {
      jdDoc.jdId
    }

    // Отработать текущий элемент
    val el0 = jdTagId2 -> jdt

    el0 #:: {
      // И отработать дочерние элементы:
      jdDoc
        .template
        .subForest
        .zipWithIndex
        .flatMap { case (subJdt, i) =>
          val jdDocCh = jdDoc.copy(
            template  = subJdt,
            jdId      = selPathRev.modify(i :: _)( jdTagId2 ),
          )
          mkTreeIndexSeg( jdDocCh )
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


  object MJdTagIdFastEq extends FastEq[MJdTagId] {
    override def eqv(a: MJdTagId, b: MJdTagId): Boolean = {
      // Список selPathRev собирается динамически на основе zipWithIndex, поэтому инстансы у него очень динамические, в основном - одноразовые.
      ((a.selPathRev ===* b.selPathRev) || (a.selPathRev ==* b.selPathRev)) &&
      // Инстансы nodeId и blockExpand берутся напрямую из стабильных JdTag, поэтому эти инстансы стабильны:
      (a.nodeId ===* b.nodeId) &&
      (a.blockExpand ===* b.blockExpand)
    }
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
                         )
  extends EmptyProduct
  with IHashCodeLazyVal
{

  override lazy val toString: String = {
    var acc: List[Any] = selPathRev
    for (bexp <- blockExpand)
      acc ::= bexp.value
    for (id <- nodeId)
      acc ::= id
    acc.mkString( HtmlConstants.MINUS )
  }

}
