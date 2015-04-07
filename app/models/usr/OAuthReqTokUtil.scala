package models.usr

import play.api.libs.oauth.RequestToken
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.15 19:25
 * Description: Вспомогательная утиль для класса play.oauth.RequestToken.
 */
object OAuthReqTokUtil {

  def TOKEN_FN   = "t"
  def SECRET_FN  = "s"

  implicit def reads: Reads[RequestToken] = (
    (__ \ TOKEN_FN).read[String] and
    (__ \ SECRET_FN).read[String]
  )(RequestToken.apply _)

  implicit def writes: Writes[RequestToken] = (
    (__ \ TOKEN_FN).write[String] and
    (__ \ SECRET_FN).write[String]
  )(unlift(RequestToken.unapply))

}
