package io.suggest.lk.m.color

import diode.FastEq
import io.suggest.color.{IColorPickerMarker, MColorData}
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 15:24
  * Description: Состояние одного color picker'а уровня одного конкретного picker'а.
  * Явно-пустая модель: состояние отображаемого picker'а.
  */

object MColorPickerS {

  /** Поддержка FastEq для инстансов [[MColorPickerS]]. */
  implicit object MColorPickerSFastEq extends FastEq[MColorPickerS] {
    override def eqv(a: MColorPickerS, b: MColorPickerS): Boolean = {
      (a.shownAt ===* b.shownAt) &&
      (a.marker ===* b.marker) &&
      (a.oldColor ===* b.oldColor)
    }
  }

  @inline implicit def univEq: UnivEq[MColorPickerS] = UnivEq.derive

}


/** Класс модели color picker'а.
  *
  * @param shownAt Если открыт пикер, то тут координата относительно viewport.
  * @param marker Опциональный маркер, когда несколько маркеров.
  * @param oldColor Старый цвет. Удобно, когда юзер цвет отключил, а потом включил.
  */
case class MColorPickerS(
                          shownAt       : MCoords2di,
                          marker        : Option[IColorPickerMarker]  = None,
                          oldColor      : Option[MColorData]          = None,
                        ) {

  def withShownAt(shownAt: MCoords2di)                = copy(shownAt = shownAt)
  def withMarker(marker: Option[IColorPickerMarker])  = copy(marker = marker)
  def withOldColor(oldColor: Option[MColorData])      = copy(oldColor = oldColor)

}
