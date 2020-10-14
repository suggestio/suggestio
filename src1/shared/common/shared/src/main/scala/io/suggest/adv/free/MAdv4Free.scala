package io.suggest.adv.free

import boopickle.Default._
import japgolly.univeq.UnivEq

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

  implicit def pickler: Pickler[MAdv4Free] = {
    implicit val propsP = MAdv4FreeProps.a4fPropsPickler
    generatePickler[MAdv4Free]
  }

  @inline implicit def univEq: UnivEq[MAdv4Free] = UnivEq.derive

}