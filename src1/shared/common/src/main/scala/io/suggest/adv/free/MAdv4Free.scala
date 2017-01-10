package io.suggest.adv.free

import boopickle.Default._

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

  implicit val pickler: Pickler[MAdv4Free] = {
    implicit val propsP = MAdv4FreeProps.pickler
    generatePickler[MAdv4Free]
  }

}