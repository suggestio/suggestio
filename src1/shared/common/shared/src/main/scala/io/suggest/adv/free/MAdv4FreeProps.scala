package io.suggest.adv.free

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

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

  /** Поддержка play-json. */
  implicit def mAdv4FreePropsFormat: OFormat[MAdv4FreeProps] = (
    (__ \ "f").format[String] and
    (__ \ "t").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MAdv4FreeProps] = UnivEq.derive

}