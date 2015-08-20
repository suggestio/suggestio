package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.ScConstants.ShowLevels._
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sc.sjs.m.msrv.ads.find.{MFindAds, MFindAdsReqDflt, MFindAdsReqEmpty}
import io.suggest.sc.sjs.vm.grid.GBlock

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

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
  }

  /** Дефолтовая реализация аргументов поиска. */
  protected class FindAdsArgs(val _sd: MStData) extends FindAdsArgsT

  /** Запуск поиска карточек. */
  protected def _findAds(sd: MStData)(implicit ec: ExecutionContext): Future[MFindAds] = {
    MFindAds.findAds(new FindAdsArgs(sd))
  }

}


trait FindAdsFsmUtil extends ScFsmStub {

  /** Подписать фьючерс на отправку результата в ScFsm. */
  protected def _sendFutResBack[T](fut: Future[T]): Unit = {
    fut onComplete { case tryRes =>
      val msg = tryRes match {
        case Success(res) => res
        case failure      => failure
      }
      // Вешать асинхронную отправку сюда смысла нет, только паразитные setTimeout() в коде появяться.
      _sendEventSyncSafe(msg)
    }
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
