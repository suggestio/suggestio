package io.suggest.adv.free

import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/** Модель diode-состояния суперюзерской формочки.
  *
  * @param checked Состояние галочки: true/false.
  */
case class MAdv4Free(
  static  : MAdv4FreeProps,
  checked : Boolean         = true
) {
  def withChecked(checked2: Boolean) = copy(checked = checked2)
}

object MAdv4Free {

  @inline implicit def univEq: UnivEq[MAdv4Free] = UnivEq.derive

  def checked = GenLens[MAdv4Free]( _.checked )

}