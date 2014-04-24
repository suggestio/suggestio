package controllers

import util.PlayMacroLogsImpl
import util.acl.IsAdnNodeAdmin
import models._
import views.html.market.lk.billing._
import play.api.db.DB
import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:23
 * Description: Контроллер управления биллингом в личном кабинете узла рекламной сети.
 */
object MarketLkBilling extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Отобразить какую-то страницу с реквизитами для платежа. */
  def paymentRequsites(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    val mbcs = DB.withConnection { implicit c =>
      MBillContract.findForAdn(adnId, isActive = Some(true))
    }
    mbcs.headOption match {
      case Some(mbc) =>
        Ok(billPaymentBankTpl(mbc))

      case None =>
        // Нет заключенных договоров, оплата невозможна.
        http404AdHoc
    }
  }


  /**
   * Рендер страницы, содержащей общую биллинговую информацию для узла.
   * @param adnId id узла.
   */
  def showAdnNodeBilling(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    val billInfoOpt = DB.withConnection { implicit c =>
      MBillContract.findForAdn(adnId, isActive = Some(true)).headOption.map { mbc =>
        val txns = MBillTxn.findForContract(mbc.id.get)
        val tariffs = MBillTariffFee.findByContractId(mbc.id.get)
        (mbc, txns, tariffs)
      }
    }
    billInfoOpt match {
      case Some((mbc, txns, tariffs)) =>
        Ok(showAdnNodeBillingTpl(request.adnNode, tariffs, txns, mbc))

      case None =>
        warn(s"showAdnNodeBilling($adnId): No active contracts found for node, but billing page requested by user ${request.pwOpt} ref=${request.headers.get("Referer")}")
        http404AdHoc
    }
  }

}
