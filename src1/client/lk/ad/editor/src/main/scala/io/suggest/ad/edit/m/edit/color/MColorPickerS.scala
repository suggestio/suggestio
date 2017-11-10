package io.suggest.ad.edit.m.edit.color

import diode.FastEq
import io.suggest.color.MColorData
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.primo.TypeT
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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
      (a.oldColor ===* b.oldColor) &&
        (a.shownAt ===* b.shownAt)
    }
  }

  implicit def univEq: UnivEq[MColorPickerS] = UnivEq.derive

}


/** Класс модели color picker'а.
  *
  * @param oldColor Старый цвет. Удобно, когда юзер цвет отключил, а потом включил.
  * @param shownAt Если открыт пикер, то тут координата относительно viewport.
  */
case class MColorPickerS(
                          oldColor      : Option[MColorData]    = None,
                          shownAt       : Option[MCoords2di]    = None
                        ) {

  def withOldColor(oldColor: Option[MColorData]) = copy(oldColor = oldColor)
  def withShownAt(shownAt: Option[MCoords2di])   = copy(shownAt = shownAt)

  def isShown = shownAt.nonEmpty

}


/** Интерфейс состояния color-picker'а с цветом фона. */
trait IBgColorPickerS extends TypeT {

  override type T <: IBgColorPickerS

  def bgColorPick: MColorPickerS

  def withBgColorPick(bgColor: MColorPickerS): T

}
