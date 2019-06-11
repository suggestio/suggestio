package io.suggest.id.reg

import io.suggest.captcha.MCaptchaCheckReq
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.19 14:13
  * Description: Модель реквеста регистрации с шага ввода капчи.
  */
object MRegCaptchaReq {

  implicit def epwRegCaptchaReqFormat: OFormat[MRegCaptchaReq] = (
    (__ \ "e").format[MRegCreds0] and
    (__ \ "a").format[MCaptchaCheckReq]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MRegCaptchaReq] = UnivEq.derive

}


/** Кросс-платформенная модель запроса регистрации по паролю.
  *
  * @param captcha Данные капчи.
  */
case class MRegCaptchaReq(
                           creds0           : MRegCreds0,
                           captcha          : MCaptchaCheckReq,
                         )
