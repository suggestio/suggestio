package io.suggest.ad.edit.m

import diode.FastEq
import io.suggest.ad.edit.m.edit.color.MColorsState
import io.suggest.ad.edit.m.edit.{MAddS, MQdEditS}
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.ad.edit.m.layout.MSlideBlocks
import io.suggest.jd.render.m.MJdArgs
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:33
  * Description: Модель состояния документа в редакторе.
  */
object MDocS {

  /** Реализация FastEq для инстансов [[MDocS]]. */
  implicit object MDocSFastEq extends FastEq[MDocS] {
    override def eqv(a: MDocS, b: MDocS): Boolean = {
      (a.jdArgs ===* b.jdArgs) &&
        (a.qdEdit ===* b.qdEdit) &&
        (a.stripEd ===* b.stripEd) &&
        (a.addS ===* b.addS) &&
        (a.colorsState ===* b.colorsState) &&
        (a.slideBlocks ===* b.slideBlocks)
    }
  }

  implicit def univEq: UnivEq[MDocS] = UnivEq.derive

}


/** Класс модели состояния работы с документом.
  *
  * @param jdArgs Текущий набор данных для рендера шаблона.
  * @param qdEdit Состояние редактирования контента, если есть.
  * @param stripEd Состояние strip-редактора, если открыт.
  * @param addS Состояние формочки добавления нового элемента.
  * @param colorsState Общее состояние редактирования цветов:
  *                    разные часто-используемые или подходящие цвета, например.
  * @param slideBlocks Состояние slide-блоков редактора.
  *                    Изначально было в layout, но оно очень активно управляется из DocEditAh.
  */
case class MDocS(
                  jdArgs        : MJdArgs,
                  qdEdit        : Option[MQdEditS]              = None,
                  stripEd       : Option[MStripEdS]             = None,
                  addS          : Option[MAddS]                 = Some(MAddS.default),
                  slideBlocks   : MSlideBlocks                  = MSlideBlocks.empty,
                  colorsState   : MColorsState                  = MColorsState.empty
                ) {

  def withJdArgs(jdArgs: MJdArgs) = copy(jdArgs = jdArgs)

  def withQdEdit(qdEdit: Option[MQdEditS]) = copy(qdEdit = qdEdit)
  def withOutQdEdit = if (qdEdit.nonEmpty) withQdEdit(None) else this

  def withStripEd(stripEd: Option[MStripEdS]) = copy(stripEd = stripEd)
  def withOutStripEd = if (stripEd.nonEmpty) withStripEd(None) else this

  def withAddS(addS: Option[MAddS]) = copy(addS = addS)

  def withColorsState(colorsState: MColorsState) = copy(colorsState = colorsState)

  def withSlideBlocks(slideBlocks: MSlideBlocks) = copy(slideBlocks = slideBlocks)

}
