package io.suggest.sc.pwa

import io.suggest.sc.{MScApiVsn, ScConstants}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:02
  * Description: Модель контейнера аргументов для манифеста.
  */
object MPwaManifestQs {

  object Fields {
    final def API_VSN_FN = ScConstants.ReqArgs.VSN_FN
  }

  @inline implicit def univEq: UnivEq[MPwaManifestQs] = UnivEq.derive

  /** Поддержка play-json. Не использовалось на момент внедрения. */
  implicit def MPWA_MANIFEST_QS_FORMAT: OFormat[MPwaManifestQs] = {
    (__ \ Fields.API_VSN_FN).format[MScApiVsn]
      .inmap[MPwaManifestQs](apply, _.apiVsn)
  }

}


/** Модель аргументов рендера манифеста.
  *
  * @param apiVsn Версия API.
  */
case class MPwaManifestQs(
                           apiVsn: MScApiVsn
                         // TODO Нужен какой-нибудь last-modified уровня URL для агрессивного кэша
                         )
