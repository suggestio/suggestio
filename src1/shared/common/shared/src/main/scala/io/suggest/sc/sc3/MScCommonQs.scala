package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.dev.MScreen
import io.suggest.geo.MLocEnv
import io.suggest.sc.{MScApiVsn, MScApiVsns, ScConstants}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.18 13:41
  * Description: Обобщённые аргументы выдачи, которые пошарены между экшенами.
  */
object MScCommonQs {

  def empty = apply()

  object Fields {
    @inline
    final def API_VSN_FN = ScConstants.ReqArgs.VSN_FN
    @inline
    final def SCREEN_FN = ScConstants.ReqArgs.SCREEN_FN
    @inline
    final def LOC_ENV_FN = ScConstants.ReqArgs.LOC_ENV_FN

    val SEARCH_GRID_ADS_FN  = "g"
    val SEARCH_TAGS_FN      = "t"
  }

  /** Поддержка play-json. */
  implicit def mScCommonQsFormat: OFormat[MScCommonQs] = (
    (__ \ Fields.API_VSN_FN).format[MScApiVsn] and
    (__ \ Fields.SCREEN_FN).formatNullable[MScreen] and
    (__ \ Fields.LOC_ENV_FN).formatNullable[MLocEnv]
      .inmap[MLocEnv](
        EmptyUtil.opt2ImplMEmptyF( MLocEnv ),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ Fields.SEARCH_GRID_ADS_FN).formatNullable[Boolean] and
    (__ \ Fields.SEARCH_TAGS_FN).formatNullable[Boolean]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MScCommonQs] = UnivEq.derive

}


/** Класс-контейнер пошаренных аргументов выдачи.
  *
  * @param screen Параметры экрана клиентского устройства.
  * @param apiVsn Версия Sc API.
  * @param locEnv Данные геолокации и физического окружения, если есть.
  * @param searchGridAds Флаг поиска и возврата разных карточек плитки в ответе.
  * @param searchNodes Флаг поиска узлов/тегов.
  */
case class MScCommonQs(
                        apiVsn            : MScApiVsn           = MScApiVsns.unknownVsn,
                        screen            : Option[MScreen]     = None,
                        locEnv            : MLocEnv             = MLocEnv.empty,
                        searchGridAds     : Option[Boolean]     = None,
                        searchNodes       : Option[Boolean]     = None,
                      ) {

  def withLocEnv(locEnv: MLocEnv) = copy(locEnv = locEnv)

}
