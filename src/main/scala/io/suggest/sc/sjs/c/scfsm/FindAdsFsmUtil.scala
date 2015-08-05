package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAds, MFindAdsReqDflt, MFindAdsReqEmpty}

import scala.concurrent.ExecutionContext

/**
 * Утиль для аддонов ScFsm для поиска карточек на сервере.
 */
trait FindAdsFsmUtil extends ScFsmStub {

  /** Дефолтовая реализация аргументов поиска. */
  protected class FindAdsArgs(sd: SD) extends MFindAdsReqEmpty with MFindAdsReqDflt {
    override def _mgs = sd.grid.state
    override val _fsmState = super._fsmState
    override def screenInfo = sd.screen
    override def generation: Option[Long] = Some(sd.generation)
    override def receiverId = sd.adnIdOpt
  }

  /** Запуск поиска карточек. */
  protected def _findAds(sd: SD = _stateData)(implicit ec: ExecutionContext) = {
    MFindAds.findAds(new FindAdsArgs(sd))
  }

}
