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
object MEpwRegCaptchaResp {

  implicit def mEpwRegCaptchaRespFormat: OFormat[MEpwRegCaptchaResp] = (
    (__ \ "n").formatNullable[String] and
    (__ \ "e").formatNullable[String],
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MEpwRegCaptchaResp] = UnivEq.derive

}


/** Контейнер данных ответа сервера.
  *
  * @param nextStepSubmitUrl Ссылка для следующего POST'а данных смс-кода.
  * @param error Код ошибки обработки реквеста.
  */
case class MEpwRegCaptchaResp(
                               nextStepSubmitUrl  : Option[String],
                               error              : Option[String],
                             )
