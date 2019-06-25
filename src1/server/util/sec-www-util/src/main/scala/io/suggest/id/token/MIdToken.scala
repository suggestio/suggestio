package io.suggest.id.token

import java.util.UUID

import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 18:42
  * Description: Модель абстрактного токена для нужд идентификации.
  */
object MIdToken {

  implicit def mIdTokenJson: OFormat[MIdToken] = (
    (__ \ "t").format[MIdTokenType] and
    (__ \ "i").format[List[MIdMsg]] and
    (__ \ "d").format[MIdTokenDates] and
    (__ \ "p").format[JsValue] and
    (__ \ "c").formatNullable[MIdTokenConstaints]
      .inmap[MIdTokenConstaints](
        EmptyUtil.opt2ImplMEmptyF(MIdTokenConstaints),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ "o").format[UUID]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MIdToken] = UnivEq.derive


  val idMsgs    = GenLens[MIdToken]( _.idMsgs )
  val payload   = GenLens[MIdToken]( _.payload )
  val dates     = GenLens[MIdToken]( _.dates )

}


/** Контейнер данных id-токена для одноразового шага идентификации.
  *
  * @param typ Тип токена, описывающий назначение.
  * @param idMsgs Месседжи идентификации: проверка капчи, смс проверки телефонного номера.
  *               Новые - добавляются в начало.
  * @param payload Произвольные данные, идущие вместе с токеном.
  * @param ottId Одноразовый уникальный идентификатор, чтобы пометить в базе токен как использованный.
  * @param dates Даты генерации токена и время жизни токена.
  * @param constraints Ограничения на использование токена.
  */
case class MIdToken(
                     typ          : MIdTokenType,
                     idMsgs       : List[MIdMsg],
                     dates        : MIdTokenDates,
                     payload      : JsValue               = JsNull,
                     constraints  : MIdTokenConstaints    = MIdTokenConstaints.empty,
                     ottId        : UUID                  = UUID.randomUUID(),
                   )

