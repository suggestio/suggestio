package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.ScConstants
import io.suggest.sc.sjs.m.mgeo.IMGeoMode
import io.suggest.sc.sjs.m.msc.MScSd
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAds, MFindAdsReqEmpty}
import io.suggest.sc.sjs.vm.grid.GBlock

import scala.concurrent.{ExecutionContext, Future}

/** Утиль для аддонов ScFsm для поиска карточек на сервере. */
trait FindAdsUtil {

  /** Дефолтовые аргументы поиска. */
  protected trait FindAdsArgsT extends MFindAdsReqEmpty {
    def _sd: MScSd
    override def screenInfo               = _sd.common.screen
    override def generation: Option[Long] = Some(_sd.common.generation)
    override def receiverId               = _sd.common.adnIdOpt
    override def levelId: Option[String]  = Some( ScConstants.ShowLevels.ID_START_PAGE )
    override def geo: Option[IMGeoMode]   = Some( IMGeoMode(_sd.geo.lastGeoLoc) )
  }

  /** Дефолтовая реализация аргументов поиска. */
  protected class FindAdsArgs(val _sd: MScSd) extends FindAdsArgsT

  /** Запуск поиска карточек. */
  protected def _findAds(sd: MScSd)(implicit ec: ExecutionContext): Future[MFindAds] = {
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
