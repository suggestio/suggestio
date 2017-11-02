package io.suggest.ad.edit.m.edit.strip

import diode.FastEq
import io.suggest.ad.edit.m.edit.color.{IBgColorPickerS, MColorPickerS}
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 22:47
  * Description: Модель props-состояния strip-редактора.
  */
object MStripEdS {

  implicit object MStripEdSFastEq extends FastEq[MStripEdS] {
    override def eqv(a: MStripEdS, b: MStripEdS): Boolean = {
      (a.isLastStrip ==* b.isLastStrip) &&
        (a.bgColorPick ===* b.bgColorPick) &&
        (a.confirmingDelete ==* b.confirmingDelete)
    }
  }

  implicit def univEq: UnivEq[MStripEdS] = UnivEq.derive

}


/** Класс модели пропертисов редактора блока.
  *
  * @param confirmingDelete Отображается подтверждение удаления блока?
  */
case class MStripEdS(
                      isLastStrip                 : Boolean,
                      override val bgColorPick    : MColorPickerS     = MColorPickerS.empty,
                      confirmingDelete            : Boolean           = false
                    )
  extends IBgColorPickerS
{

  override type T = MStripEdS

  def withConfirmDelete(confirmDelete: Boolean) = copy(confirmingDelete = confirmDelete)
  override def withBgColorPick(bgColorPick: MColorPickerS) = copy(bgColorPick = bgColorPick)

}
