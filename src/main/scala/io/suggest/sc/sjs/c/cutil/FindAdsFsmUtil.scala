package io.suggest.sc.sjs.c.cutil

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAds, MFindAdsReqDflt, MFindAdsReqEmpty}

import scala.concurrent.ExecutionContext

/**
 * Утиль для аддонов ScFsm для поиска карточек на сервере.
 */
trait FindAdsFsmUtil extends ScFsmStub {

  /** Дефолтовая реализация аргументов поиска. */
  protected class FindAdsArgs(sd: SD) extends MFindAdsReqEmpty with MFindAdsReqDflt {
    override def _mgs = sd.gridState
    override val _fsmState = super._fsmState
    override def screenInfo: Option[IMScreen] = sd.screen
    override def generation: Option[Long] = Some(sd.generation)
  }

  /** Запуск поиска карточек. */
  protected def _findAds(sd: SD = _stateData)(implicit ec: ExecutionContext) = MFindAds.findAds(new FindAdsArgs(sd))

}
