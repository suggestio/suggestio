package io.suggest.id.reg

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.19 14:13
  * Description: Модель реквеста регистрации с шага ввода капчи.
  */
object MEpwRegCaptchaReq {

  implicit def epwRegCaptchaReqFormat: OFormat[MEpwRegCaptchaReq] = (
    (__ \ "e").format[String] and
    (__ \ "t").format[String] and
    (__ \ "c").format[String] and
    (__ \ "s").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MEpwRegCaptchaReq] = UnivEq.derive

}


/** Кросс-платформенная модель запроса регистрации по паролю.
  *
  * @param email Адрес email.
  * @param phone Номер телефона.
  * @param captchaTyped Введённая пользователем капча.
  * @param captchaSecret Секретный шифротекст капчи.
  */
case class MEpwRegCaptchaReq(
                              email            : String,
                              phone            : String,
                              captchaTyped     : String,
                              captchaSecret    : String,
                            )
