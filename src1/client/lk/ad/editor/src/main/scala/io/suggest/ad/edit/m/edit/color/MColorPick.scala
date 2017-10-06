package io.suggest.ad.edit.m.edit.color

import diode.FastEq
import io.suggest.color.MColorData
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 16:10
  * Description: Модель для связывания компонентов выбора цвета и внешней системы.
  * Явно пустая, ориентирована на использование внутри Option[].
  */
object MColorPick {

  /** Поддержка FastEq для инстансов [[MColorPick]]. */
  implicit object MColorPickFastEq extends FastEq[MColorPick] {
    override def eqv(a: MColorPick, b: MColorPick): Boolean = {
      (a.colorOpt ===* b.colorOpt) &&
        (a.colorsState ===* b.colorsState) &&
        (a.pickS ===* b.pickS)
    }
  }

  implicit def univEq: UnivEq[MColorPick] = UnivEq.derive

}


/** Класс контейнера данных для контроллера [[io.suggest.ad.edit.c.ColorPickAh]].
  *
  * @param colorOpt Текущее состояние цвета.
  * @param colorsState Пошаренное состояние всех цветов.
  * @param pickS Состояние конкретного color picker'а.
  */
case class MColorPick(
                         colorOpt     : Option[MColorData],
                         colorsState  : MColorsState,
                         pickS        : MColorPickerS
                       ) {

  def withColorOpt(colorOpt: Option[MColorData]) = copy(colorOpt = colorOpt)
  def withColorsState(colorsState: MColorsState) = copy(colorsState = colorsState)
  def withPickS(pickS: MColorPickerS) = copy(pickS = pickS)

}
