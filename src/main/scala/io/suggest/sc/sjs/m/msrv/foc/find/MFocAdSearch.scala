package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAdsReqWrapper, MFindAdsReqEmpty, MFindAdsReq}
import io.suggest.ad.search.AdSearchConstants._

import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:07
 * Description: Модель расширения к FindAds
 */
trait MFocAdSearch extends MFindAdsReq {

  /** id рекламной карточки для активации автопереброса на выдачу продьюсера этой карточки. */
  def openIndexAdId: Option[String]

  /** id рекламной карточки, на базе которой надо вычислить стартовые параметры foc-выдачи, т.к. они неизвестны. */
  def adIdLookup: Option[String]

  override def toJson: Dictionary[Any] = {
    val acc = super.toJson

    for (_openIndexAdId <- openIndexAdId)
      acc(OPEN_INDEX_AD_ID_FN) = _openIndexAdId

    for (adId <- adIdLookup)
      acc(AD_ID_LOOKUP_FN) = adId

    acc
  }

}


/** Дефолтовая реализация модели с пустыми значениями полей. */
trait MFocAdSearchEmpty extends MFindAdsReqEmpty with MFocAdSearch {

  override def openIndexAdId  : Option[String] = None
  override def adIdLookup     : Option[String] = None

}


/** Параметры поиска с запретом на открытие карточек. */
trait MFocAdSearchNoOpenIndex extends MFocAdSearch {
  // При любом раскладе сервер не должен возвращать index-ответ.
  override final def openIndexAdId: Option[String] = None
}


/** Враппер для заворачивания экземпляров модели. */
trait MFocAdSearchWrapper extends MFindAdsReqWrapper with MFocAdSearch {

  override def _underlying: MFocAdSearch

  override def openIndexAdId  = _underlying.openIndexAdId
  override def adIdLookup     = _underlying.adIdLookup

}
