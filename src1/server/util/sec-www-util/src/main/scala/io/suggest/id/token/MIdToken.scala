package io.suggest.id.token

import java.time.Instant
import java.util.UUID

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
    (__ \ "o").format[UUID] and
    (__ \ "c").format[Instant]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIdToken] = UnivEq.derive

}


/** Контейнер данных id-токена для одноразового шага идентификации.
  *
  * @param typ Тип токена, описывающий назначение.
  * @param info Месседжи и прочие совершенные действия идентификации.
  * @param payload Произвольные данные, идущие вместе с токеном.
  * @param ott Одноразовый уникальный идентификатор, чтобы пометить в базе токен как использованный.
  * @param created Дата генерации токена.
  */
case class MIdToken(
                     typ          : MIdTokenType,
                     info         : MIdTokenInfo,
                     payload      : JsValue,
                     ott          : UUID            = UUID.randomUUID(),
                     created      : Instant         = Instant.now(),
                   )
