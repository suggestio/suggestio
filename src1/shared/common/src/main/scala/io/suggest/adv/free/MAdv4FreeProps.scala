package io.suggest.adv.free

import boopickle.Default._

/** Статическая инфа по бесплатному размещению для суперюзеров.
  *
  * @param fn Имя form-поля: "freeAdv"
  * @param title Текст галочки: "Размещать бесплатно, без подтверждения?"
  */
case class MAdv4FreeProps(
  fn      : String,
  title   : String
)

object MAdv4FreeProps {
  implicit val a4fPropsPickler: Pickler[MAdv4FreeProps] = {
    generatePickler[MAdv4FreeProps]
  }
}