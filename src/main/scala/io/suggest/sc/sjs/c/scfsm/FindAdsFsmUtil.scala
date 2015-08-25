package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.ScConstants.ShowLevels._
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAds, MFindAdsReqDflt, MFindAdsReqEmpty}
import io.suggest.sc.sjs.vm.grid.GBlock

import scala.concurrent.{Future, ExecutionContext}

/** Утиль для аддонов ScFsm для поиска карточек на сервере. */
trait FindAdsUtil {

  /** Дефолтовые аргументы поиска. */
  protected trait FindAdsArgsT extends MFindAdsReqEmpty with MFindAdsReqDflt {
    def _sd: MStData
    override def _mgs                     = _sd.grid.state
    override def screenInfo               = _sd.screen
    override def generation: Option[Long] = Some(_sd.generation)
    override def receiverId               = _sd.adnIdOpt
    override def levelId: Option[String]  = Some(ID_START_PAGE)
    override def geo: Option[IMGeoMode]   = Some( _sd.geo.currGeoMode )
  }

  /** Дефолтовая реализация аргументов поиска. */
  protected class FindAdsArgs(val _sd: MStData) extends FindAdsArgsT

  /** Запуск поиска карточек. */
  protected def _findAds(sd: MStData)(implicit ec: ExecutionContext): Future[MFindAds] = {
    MFindAds.findAds(new FindAdsArgs(sd))
  }

}


/** Трейт с утилью для поиска соседних (по плитке) карточек. */
trait FindNearAdIds {

  protected def _nearAdIdsIter(gblockOpt: Option[GBlock]): Iterator[String] = {
    gblockOpt.iterator
      .flatMap { gblock =>
        gblock.previous.iterator ++ Iterator(gblock) ++ gblock.next.iterator
      }
      .flatMap { _.madId }
  }

}
