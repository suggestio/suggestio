package io.suggest.sc.sjs.m.msc

import io.suggest.sc.sjs.m.mgeo.{IMGeoMode, MGeoPoint}
import io.suggest.sc.sjs.m.mgrid.MFindGridAdsArgsLimitOffsetT
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAdsReqDflt

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.07.16 17:56
  * Description: Вспомогательные модели аргументов запроса.
  */

/** Заполнить аргументы поиска карточек на основе данных состояния [[MScSd]] от ScFsm. */
trait MFindAdsArgsT extends MFindAdsReqDflt {

  /** Текущее состояние ScFsm. */
  def _sd: MScSd

  override def screenInfo               = Some( _sd.common.screen )
  override def generation: Option[Long] = Some(_sd.common.generation)
  override def receiverId               = _sd.common.adnIdOpt
  //override def levelId: Option[String]  = Some( ScConstants.ShowLevels.ID_START_PAGE )
  override def geo: Option[IMGeoMode]   = Some( IMGeoMode(_sd.geo.lastGeoLoc) )
  override def agpPoint: Option[MGeoPoint] = _sd.common.geoLocOpt

}


/** Расширенный вариант [[MFindAdsArgsT]] с заполнением значений limit и offset. */
trait MFindAdsArgsLimOffT extends MFindAdsArgsT with MFindGridAdsArgsLimitOffsetT {
  override def _mgs = _sd.grid.state
}

case class MFindAdsArgsLimOff(override val _sd: MScSd)
  extends MFindAdsArgsLimOffT
