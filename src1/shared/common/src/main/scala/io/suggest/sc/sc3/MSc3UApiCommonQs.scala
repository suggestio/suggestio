package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.dev.MScreen
import io.suggest.geo.MLocEnv
import io.suggest.sc.{MScApiVsn, ScConstants}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.18 13:41
  * Description: Обобщённые аргументы выдачи, которые пошарены между экшенами.
  */
object MSc3UApiCommonQs {

  object Fields {
    @inline
    final def API_VSN_FN = ScConstants.ReqArgs.VSN_FN
    @inline
    final def SCREEN_FN = ScConstants.ReqArgs.SCREEN_FN
    @inline
    final def LOC_ENV_FN = ScConstants.ReqArgs.LOC_ENV_FN
  }

  /** Поддержка play-json. */
  implicit def mSc3UApiCommonQsFormat: OFormat[MSc3UApiCommonQs] = (
    (__ \ Fields.API_VSN_FN).format[MScApiVsn] and
    (__ \ Fields.SCREEN_FN).formatNullable[MScreen] and
    (__ \ Fields.LOC_ENV_FN).formatNullable[MLocEnv]
      .inmap[MLocEnv](
        EmptyUtil.opt2ImplMEmptyF( MLocEnv ),
        EmptyUtil.implEmpty2OptF
      )
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MSc3UApiCommonQs] = UnivEq.derive

}


/** Класс-контейнер пошаренных аргументов выдачи.
  *
  * @param screen Параметры экрана клиентского устройства.
  * @param apiVsn Версия Sc API.
  * @param locEnv Данные геолокации и физического окружения, если есть.
  */
case class MSc3UApiCommonQs(
                             apiVsn     : MScApiVsn,
                             screen     : Option[MScreen],
                             locEnv     : MLocEnv         = MLocEnv.empty,
                           )
