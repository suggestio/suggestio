package io.suggest.id.token

import java.time.Instant

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 18:42
  * Description: Модель абстрактного токена для нужд идентификации.
  */
object MIdToken {

  implicit def mIdTokenJson: OFormat[MIdToken] = (
    (__ \ "t").format[MIdTokenType] and
    (__ \ "i").format[MIdTokenInfo] and
    (__ \ "p").format[JsValue] and
    (__ \ "s").formatNullable[String] and
    (__ \ "c").format[Instant]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIdToken] = UnivEq.derive

}


case class MIdToken(
                     typ          : MIdTokenType,
                     info         : MIdTokenInfo,
                     payload      : JsValue,
                     sessionId    : Option[String]  = None,
                     created      : Instant         = Instant.now(),
                   )
