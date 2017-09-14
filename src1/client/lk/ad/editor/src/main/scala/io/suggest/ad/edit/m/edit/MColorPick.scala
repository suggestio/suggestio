package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.model.n2.node.meta.colors.MColorData

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
      (a.colorOpt eq b.colorOpt) &&
        (a.colorsState eq b.colorsState) &&
        (a.pickS eq b.pickS)
    }
  }

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
