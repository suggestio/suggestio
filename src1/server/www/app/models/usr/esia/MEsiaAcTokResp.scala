package models.usr.esia

import java.util.UUID

import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.19 11:48
  * Description: JSON-модель положительного ответа ЕСИА с данным access_token.
  */
object MEsiaAcTokResp {

  @inline implicit def univEq: UnivEq[MEsiaAcTokResp] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def esiaAcTokRespFormat: OFormat[MEsiaAcTokResp] = (
    (__ \ "id_token").formatNullable[String] and
    (__ \ "access_token").formatNullable[String] and
    (__ \ "expires_in").formatNullable[Int] and
    (__ \ "token_type").formatNullable[MEsiaTokenType] and
    (__ \ "refresh_token").formatNullable[String] and
    (__ \ "state").formatNullable[UUID]
  )(apply, unlift(unapply))

}


case class MEsiaAcTokResp(
                           idToken        : Option[String],
                           accessToken    : Option[String],
                           expiresInSec   : Option[Int],
                           tokenType      : Option[MEsiaTokenType],
                           refreshToken   : Option[String],
                           state          : Option[UUID],
                         )
