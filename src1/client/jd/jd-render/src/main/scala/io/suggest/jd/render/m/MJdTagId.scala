package io.suggest.jd.render.m

import io.suggest.ad.blk.MBlockExpandMode
import io.suggest.common.html.HtmlConstants
import io.suggest.scalaz.NodePath_t
import japgolly.scalajs.react.Key
import japgolly.scalajs.react.vdom.Attr.ValueType
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

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

  val selPath = GenLens[MJdTagId](_.selPath)

}


/** Класс-контейнер данных идентификатора блока.
  *
  * @param nodeId id узла-карточки, если есть.
  * @param selPath Порядковый номер до тега внутри jd-документа (одной карточки).
  *                Эквивалентен по смыслу к MJdRenderArgs.selPath
  * @param blockExpand Режим отображения блока.
  */
final case class MJdTagId(
                           nodeId       : Option[String],
                           selPath      : NodePath_t,
                           blockExpand  : Option[MBlockExpandMode],
                         ) {

  override val toString: String = {
    var acc: List[Any] = selPath
    for (bexp <- blockExpand)
      acc ::= bexp.value
    for (id <- nodeId)
      acc ::= id
    acc.mkString( HtmlConstants.MINUS )
  }

}
