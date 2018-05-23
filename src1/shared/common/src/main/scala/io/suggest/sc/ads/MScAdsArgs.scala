package io.suggest.sc.ads

import io.suggest.common.empty.EmptyUtil
import io.suggest.dev.MScreen
import io.suggest.sc.{MScApiVsn, ScConstants}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.18 23:10
  * Description: Общая модель аргументов реквеста карточек.
  */
object MScAdsArgs {

  implicit def univEq: UnivEq[MScAdsArgs] = UnivEq.derive

  /** Поддержка play-json. */
  implicit def mScAdsArgsFormat: OFormat[MScAdsArgs] = (
    (__ \ "s").formatNullable[MFindAdsReq]
      .inmap[MFindAdsReq](
        EmptyUtil.opt2ImplMEmptyF( MFindAdsReq ),
        EmptyUtil.implEmpty2OptF
      ) and
    (__ \ "f").formatNullable[MScFocusArgs] and
    // Для экрана только s или screen допустимо, см. Context.scala.
    (__ \ ScConstants.ReqArgs.SCREEN_FN).formatNullable[MScreen] and
    (__ \ ScConstants.ReqArgs.VSN_FN).format[MScApiVsn]
  )(apply, unlift(unapply))

}


/**
  *
  * @param search Поисковые аргументы. Неявно-пустая модель.
  * @param foc Параметры фокусировки на карточке в случае
  * @param screen Параметры экрана клиентского устройства.
  * @param apiVsn Версия Sc API.
  */
case class MScAdsArgs(
                       search    : MFindAdsReq,
                       foc       : Option[MScFocusArgs],
                       screen    : Option[MScreen],
                       apiVsn    : MScApiVsn
                     )
