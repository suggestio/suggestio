package controllers

import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import models._
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import play.api.db.DB
import play.api.Play.current
import views.html.sys1.market.billing._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 12:39
 * Description: Контроллер управления биллинга для операторов sio-market.
 */
object SysMarketBilling extends SioController with PlayMacroLogsImpl {

  /** Страница с информацией по биллингу. */
  def billingFor(adnId: String) = IsSuperuser.async { implicit request =>
    val adnNodeOptFut = MAdnNode.getById(adnId)
    // Синхронные модели
    val (_balanceOpt, _contracts, _txns) = DB.withConnection { implicit c =>
      val balanceOpt = MBillBalance.getByAdnId(adnId)
      val contracts = MBillContract.findForAdn(adnId)
      val contractIds = contracts.map(_.id.get)
      val txns = MBillTxn.findForContracts(contractIds)
      (balanceOpt, contracts, txns)
    }
    adnNodeOptFut map {
      case Some(adnNode) =>
        Ok(adnNodeBillingTpl(adnNode, _balanceOpt, _contracts, _txns))
      case None =>
        http404AdHoc
    }
  }

  /** Форма создания нового контракта (договора). */
  def createContractForm(adnId: String) = IsSuperuser.async { implicit request =>
    ???
  }

  /** Сабмит формы создания нового контакта (договора). */
  def createContractFormSubmit(adnId: String) = IsSuperuser.async { implicit request =>
    ???
  }
}
