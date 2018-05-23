package io.suggest.sc.sc3

import io.suggest.sc.ads.{MFindAdsReq, MScAdsArgs}
import io.suggest.sc.index.MScIndexArgs
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.18 13:14
  * Description: Модель-контейнер для всех реквестов, идущих через Sc UApi.
  */
object MSc3UApiQs {

  object Fields {
    val COMMON_FN = "c"
    val GRID_ADS_FN = "g"
    val INDEX_FN = "i"
    val FOCUSED_ADS_FN = "f"
  }

  /** Поддержка json. Не ясно, нужна ли.
    * Для возможной генерации ссылок на клиенте - будет нужна. */
  implicit def mSc3UapiQsFormat: OFormat[MSc3UApiQs] = (
    (__ \ Fields.COMMON_FN).format[MSc3UApiCommonQs] and
    (__ \ Fields.GRID_ADS_FN).formatNullable[MFindAdsReq] and
    (__ \ Fields.INDEX_FN).formatNullable[MScIndexArgs] and
    (__ \ Fields.FOCUSED_ADS_FN).formatNullable[MScAdsArgs]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MSc3UApiQs] = UnivEq.derive

}


/** Класс-контейнер qs-аргументов для Sc3 UApi, позволяющего делать сложный запрос к Sc-контроллеру.
  *
  * @param ads Запрос поиска grid-ads
  * @param index Запрос index
  * @param foc Запрос фокусировки.
  * @param common Пошаренные части запросов.
  */
case class MSc3UApiQs(
                       common : MSc3UApiCommonQs,
                       ads    : Option[MFindAdsReq]   = None,
                       index  : Option[MScIndexArgs]  = None,
                       foc    : Option[MScAdsArgs]    = None,
                     )
