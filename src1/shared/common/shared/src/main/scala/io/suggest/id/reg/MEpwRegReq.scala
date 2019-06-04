package io.suggest.id.reg

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.19 14:13
  * Description: Модель реквеста регистрации.
  */
object MEpwRegReq {

  implicit def epwRegReqFormat: OFormat[MEpwRegReq] = (
    (__ \ "e").format[String] and
    (__ \ "c").format[String] and
    (__ \ "s").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MEpwRegReq] = UnivEq.derive

}


/** Кросс-платформенная модель запроса регистрации по паролю.
  *
  * @param email Адрес email.
  * @param captchaTyped Введённая пользователем капча.
  * @param captchaSecret Секретный шифротекст капчи.
  */
case class MEpwRegReq(
                       email            : String,
                       captchaTyped     : String,
                       captchaSecret    : String,
                     )
