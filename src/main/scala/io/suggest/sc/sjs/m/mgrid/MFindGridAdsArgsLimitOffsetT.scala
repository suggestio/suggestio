package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.sjs.m.msrv.ads.find.MFindAdsReq

/** Дополнить аргументы поиска карточек данными плитки. */
trait MFindGridAdsArgsLimitOffsetT extends MFindAdsReq {

  def _mgs: MGridState

  override def limit : Option[Int]          = Some(_mgs.adsPerLoad)
  override def offset: Option[Int]          = Some(_mgs.blocksLoaded)

}
