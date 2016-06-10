package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAdsReq, MFindAdsReqEmpty}
import io.suggest.ad.search.AdSearchConstants._
import io.suggest.sjs.common.model.mlu.MLookupMode

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:07
 * Description: Модель расширения к FindAds
 */
trait MFocAdSearch extends MFindAdsReq {

  /**
    * Флаг, сообщающий серверу о допустимости возврата index-ответа или
    * иного переходного ответа вместо focused json.
    */
  def allowReturnJump: Boolean

  /** Задание режима lookup'а карточек. */
  def adsLookupMode: MLookupMode

  /** id базовой рекламной карточки, относительно которой необходимо искать сегмент. */
  def adIdLookup: String

  override def toJson: Dictionary[Any] = {
    val acc = super.toJson

    acc(OPEN_INDEX_AD_ID_FN)  = allowReturnJump
    acc(AD_LOOKUP_MODE_FN)    = adsLookupMode.strId
    acc(AD_ID_LOOKUP_FN)      = adIdLookup

    acc
  }

}


/** Дефолтовая реализация модели с пустыми значениями полей. */
trait MFocAdSearchEmpty extends MFindAdsReqEmpty with MFocAdSearch {
}


/** Параметры поиска с запретом на открытие карточек. */
trait MFocAdSearchNoOpenIndex extends MFocAdSearch {
  // При любом раскладе сервер не должен возвращать index-ответ.
  override final def allowReturnJump = false
}
