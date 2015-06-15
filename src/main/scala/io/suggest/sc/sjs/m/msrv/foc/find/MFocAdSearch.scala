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

  /** id продьюсера последней карточки из предыдущей последовательности. */
  def lastProducerId: Option[String]

  override def toJson: Dictionary[Any] = {
    val acc = super.toJson

    val _ppi = lastProducerId
    if (_ppi.nonEmpty)
      acc.update(LAST_PROD_ID_FN, _ppi.get)

    acc
  }
}


/** Дефолтовая реализация модели с пустыми значениями полей. */
trait MFocAdSearchEmpty extends MFindAdsReqEmpty with MFocAdSearch {

  override def lastProducerId: Option[String] = None

}


/** Враппер для заворачивания экземпляров модели. */
trait MFocAdSearchWrapper extends MFindAdsReqWrapper with MFocAdSearch {

  override def _underlying: MFocAdSearch

  override def lastProducerId = _underlying.lastProducerId
}
