package controllers.sysctl.bill

import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.balance.{IMBalances, MBalance}
import io.suggest.mbill2.m.contract.IMContracts
import models.msys.bill.MForNodeTplArgs
import util.PlayMacroLogsI
import util.acl.IsSuNode
import util.billing.ITfDailyUtilDi
import views.html.sys1.bill._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 14:05
 * Description: Трейт с экшенами sys-биллинга узла второго поколения.
 */
trait SbNode
  extends IsSuNode
  with PlayMacroLogsI
  with IMContracts
  with IMBalances
{

  import mCommonDi._

  /**
   * Отображение страницы биллинга для узла.
   * @param nodeId id просматриваемого узла.
   */
  def forNode(nodeId: String) = IsSuNode(nodeId).async { implicit request =>
    val contractIdOpt = request.mnode.billing.contractId

    val mContractOptFut = FutureUtil.optFut2futOpt(contractIdOpt) { contractId =>
      val mContractOptAction = mContracts.getById(contractId)
      dbConfig.db.run(mContractOptAction)
    }

    val mBalancesFut = contractIdOpt.fold[Future[Seq[MBalance]]] {
      Future.successful(Nil)
    } { contractId =>
      val mBalancesAction = mBalances.findByContractId(contractId)
      dbConfig.db.run(mBalancesAction)
    }

    for {
      mContractOpt <- mContractOptFut
      mBalances    <- mBalancesFut
    } yield {
      // Поискать контракт, собрать аргументы для рендера, отрендерить forNodeTpl.
      val args = MForNodeTplArgs(
        mnode         = request.mnode,
        mContractOpt  = mContractOpt,
        mBalances     = mBalances
      )
      Ok( forNodeTpl(args) )
    }
  }

}
