package io.suggest.sc.index

import io.suggest.sc.ScConstants.ReqArgs._
import japgolly.univeq._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.07.17 18:43
  * Description: Модель аргументов для получения index'а.
  *
  * В отличие от sc2, здесь всё immutable, без трейтов, и используется play-json для JSON-сериализации.
  *
  * Потом, этот же JSON можно будет запихать в websocket или куда-нибудь ещё.
  * Или в бинарный msgpack даже.
  */
object MScIndexArgs {

  def default = apply()

  /** Поддержка JSON-сериализации */
  implicit def mscIndexArgsFormat: OFormat[MScIndexArgs] = (
    // Всегда нужен хотя бы один ОБЯЗАТЕЛЬНЫЙ (не опциональный) формат. Иначе ScIndex будет неправильно начнётся.
    (__ \ NODE_ID_FN).formatNullable[String] and
    (__ \ GEO_INTO_RCVR_FN).format[Boolean] and
    (__ \ RET_GEO_LOC_FN).format[Boolean]
  )( apply, unlift(unapply) )

  @inline implicit def univEq: UnivEq[MScIndexArgs] = UnivEq.derive

}


/** Класс модели аргументов запросов sc index с сервера.
  *
  * @param nodeId id узла-ресивера, если есть.
  * @param geoIntoRcvr Допускать ли вход в ресивер по гео-координатам?
  *                    true на стадии геолокации с поиском узла
  *                    При гулянии по карте - false
  *                    При клике по узлу на карте - true.
  * @param retUserLoc Возвращать ли назад геолокацию юзера, принятую сервером?
  *                   true, пока геолокация неизвестна вообще, и сервер может её подсказать.
  *                   false в остальных случаях.
  */
case class MScIndexArgs(
                         nodeId       : Option[String]  = None,
                         geoIntoRcvr  : Boolean         = true,
                         retUserLoc   : Boolean         = false,
                       )
