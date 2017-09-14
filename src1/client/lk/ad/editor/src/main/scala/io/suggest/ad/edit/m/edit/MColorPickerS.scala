package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.model.n2.node.meta.colors.MColorData

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 15:24
  * Description: Состояние одного color picker'а уровня одного конкретного picker'а.
  */

object MColorPickerS {

  def empty = MColorPickerS()

  /** Поддержка FastEq для инстансов [[MColorPickerS]]. */
  implicit object MColorPickerSFastEq extends FastEq[MColorPickerS] {
    override def eqv(a: MColorPickerS, b: MColorPickerS): Boolean = {
      (a.oldColor eq b.oldColor) &&
        (a.isShown == b.isShown)
    }
  }

}


/** Класс модели color picker'а.
  *
  * @param oldColor Старый цвет. Удобно, когда юзер цвет отключил, а потом включил.
  * @param isShown Открыт ли picker сейчас?
  */
case class MColorPickerS(
                          oldColor      : Option[MColorData]    = None,
                          isShown       : Boolean               = false
                        ) {

  def withOldColor(oldColor: Option[MColorData]) = copy(oldColor = oldColor)
  def withIsShown(isShown: Boolean) = copy(isShown = isShown)

  def withIsShownInverted = withIsShown( !isShown )

}
