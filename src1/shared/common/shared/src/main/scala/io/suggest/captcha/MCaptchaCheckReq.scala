package io.suggest.captcha

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.06.19 10:34
  * Description: Юзер ввёл капчу, и запрос верификации капчи отправляется на сервер в этой json-модели.
  */
object MCaptchaCheckReq {

  /** Поддержка play-json. */
  implicit def mCaptchaCheckReqJson: OFormat[MCaptchaCheckReq] = (
    (__ \ "s").format[String] and
    (__ \ "t").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MCaptchaCheckReq] = UnivEq.derive

}


/** Контейнер данных запроса проверки капчи.
  *
  * @param secret Серверный секрет капчи.
  * @param typed Что ввёл юзер в качестве отгадки.
  */
case class MCaptchaCheckReq(
                             secret    : String,
                             typed     : String,
                           )
