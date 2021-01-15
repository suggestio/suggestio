package io.suggest.lk.m.color

import diode.FastEq
import io.suggest.color.MColorData
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

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
      (a.colorsState ===* b.colorsState)
    }
  }

  @inline implicit def univEq: UnivEq[MColorPick] = UnivEq.derive

  def colorOpt = GenLens[MColorPick]( _.colorOpt )
  def colorsState = GenLens[MColorPick]( _.colorsState )

}


/** Класс контейнера данных для контроллера [[io.suggest.lk.c.ColorPickAh]].
  *
  * @param colorOpt Текущее состояние цвета.
  * @param colorsState Пошаренное состояние всех цветов.
  */
final case class MColorPick(
                             colorOpt     : Option[MColorData],
                             colorsState  : MColorsState,
                           )
