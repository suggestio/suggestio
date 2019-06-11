package io.suggest.id.reg

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 17:41
  * Description: JSON-модель ответ сервера на шаге ввода капчи.
  */
object MRegTokenResp {

  implicit def mEpwRegCaptchaRespFormat: OFormat[MRegTokenResp] = {
    (__ \ "t").format[String]
      .inmap[MRegTokenResp]( apply, _.token )
  }

  @inline implicit def univEq: UnivEq[MRegTokenResp] = UnivEq.derive

}


/** Контейнер данных ответа сервера.
  *
  * @param token Данные для POST'а с проверкой смс-кода
  *              или повторной отправкой смс-кода.
  */
case class MRegTokenResp(
                               token  : String,
                             )
