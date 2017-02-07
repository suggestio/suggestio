package controllers.sysctl.bill

import controllers.SioController
import io.suggest.mbill2.m.balance.IMBalances
import io.suggest.mbill2.m.gid.IGidUtilDi
import io.suggest.mbill2.m.txn.IMTxns
import models.msys.bill.MBillOverviewTplArgs
import util.acl.IIsSu
import views.html.sys1.bill._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.02.16 11:30
  * Description: Аддон для контроллера [[controllers.SysBilling]] для добавления вне-узловых экшенов биллинга.
  * Изначально, тут был только обзор биллинга. Возможно, потом появятся ещё экшены.
  */
trait SbOverview
  extends SioController
  with IIsSu
  with IMTxns
  with IMBalances
  with IGidUtilDi
{

  import mCommonDi._

  /**
    * Обзор биллинга второго поколения. Пришел на смену ctl.SysMarketBilling.index().
    *
    * @return 200 Ok со страницей инфы по биллингу.
    */
  def overview = isSu.Get.async { implicit request =>
    // Поиск последних финансовых транзакций для отображения таблицы оных.
    val txnsBalancesFut = slick.db.run {
      for {
        txns      <- mTxns.findLatestTxns(limit = 10)
        if txns.nonEmpty
        balances  <- mBalances.getByIds( txns.iterator.map(_.balanceId).toTraversable )
      } yield {
        (txns, balances)
      }
    }.recover { case _: NoSuchElementException =>
      (Nil, Nil)
    }

    val balancesMapFut = for ((_, balances) <- txnsBalancesFut) yield {
      gidUtil.elements2map(balances)
    }

    // Запустить рендер результата
    for {
      (txns, _)   <- txnsBalancesFut
      balancesMap <- balancesMapFut
    } yield {
      val args = MBillOverviewTplArgs(
        txns          = txns,
        balancesMap   = balancesMap
      )
      Ok(overviewTpl(args))
    }
  }

}
