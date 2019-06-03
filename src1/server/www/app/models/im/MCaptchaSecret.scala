package models.im

import java.util.UUID

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.06.19 18:26
  * Description: JSON-модель хранения данных капчи: id, разгадку, возможно что-то ещё.
  */
object MCaptchaSecret {

  implicit def captchaSecretFormat: OFormat[MCaptchaSecret] = (
    (__ \ "i").format[UUID] and
    (__ \ "t").format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MCaptchaSecret] = UnivEq.derive

}

case class MCaptchaSecret(
                           captchaUid   : UUID,
                           captchaText  : String,
                         )
