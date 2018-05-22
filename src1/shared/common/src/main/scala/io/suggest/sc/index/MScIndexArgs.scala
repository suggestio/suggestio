package io.suggest.sc.index

import io.suggest.common.empty.EmptyUtil
import io.suggest.dev.MScreen
import io.suggest.geo.MLocEnv
import io.suggest.sc.MScApiVsn
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

  /** Поддержка JSON-сериализации */
  implicit def mscIndexArgsFormat: OFormat[MScIndexArgs] = (
    (__ \ NODE_ID_FN).formatNullable[String] and
    (__ \ LOC_ENV_FN).formatNullable[MLocEnv]
      .inmap[MLocEnv](
        EmptyUtil.opt2ImplMEmptyF(MLocEnv),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ SCREEN_FN).formatNullable[MScreen] and
    (__ \ WITH_WELCOME_FN).format[Boolean] and
    (__ \ GEO_INTO_RCVR_FN).format[Boolean] and
    (__ \ VSN_FN).format[MScApiVsn]
  )( apply, unlift(unapply) )

  implicit def univEq: UnivEq[MScIndexArgs] = UnivEq.derive

}


/** Класс модели аргументов запросов sc index с сервера.
  *
  * @param nodeId id узла-ресивера, если есть.
  * @param locEnv Описание текущей локации устройства (неявно-пустая модель).
  * @param screen Описание экрана устройства.
  * @param withWelcome Планируется ли рендерить splash-screen приветствия?
  * @param geoIntoRcvr Допускать ли вход в ресивер по гео-координатам?
  *                    true на стадии геолокации с поиском узла
  *                    При гулянии по карте - false
  *                    При клике по узлу на карте - true.
  */
case class MScIndexArgs(
                         nodeId       : Option[String]  = None,
                         locEnv       : MLocEnv         = MLocEnv.empty,
                         screen       : Option[MScreen] = None,
                         withWelcome  : Boolean         = true,
                         geoIntoRcvr  : Boolean         = true,
                         apiVsn       : MScApiVsn
                       )
