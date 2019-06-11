package io.suggest.id.reg

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.06.19 14:58
  * Description: Модель данных регистрации нулевого шага.
  */
object MRegCreds0 {

  implicit def mRegCreds0Json: OFormat[MRegCreds0] = (
    (__ \ "e").format[String] and
    (__ \ "t").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MRegCreds0] = UnivEq.derive

}


/** Контейнер данных регистрации нулевого шага.
  *
  * @param email Адрес email.
  * @param phone Номер телефона.
  */
case class MRegCreds0(
                       email      : String,
                       phone      : String,
                     )
