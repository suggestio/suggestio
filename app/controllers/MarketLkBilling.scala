package controllers

import util.PlayMacroLogsImpl
import util.acl._
import models._
import views.html.market.lk.billing._
import play.api.db.DB
import play.api.Play.current
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

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
  def showAdnNodeBilling(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val isProducer = request.adnNode.adn.isProducer
    val otherRcvrsFut = if (isProducer) {
      MAdnNode.findByAllAdnRights(Seq(AdnRights.RECEIVER))
        .map { _.filter(_.id.get != adnId).sortBy(_.meta.name) }
    } else {
      Future successful Nil
    }
    val billInfoOpt = DB.withConnection { implicit c =>
      MBillContract.findForAdn(adnId, isActive = Some(true))
        .headOption
        .map { mbc =>
          val contractId = mbc.id.get
          val txns = MBillTxn.findForContract(contractId)
          // Если этот узел - приёмник рекламы, то нужно найти в базе его тарифные планы.
          val myMbmds = if (request.adnNode.adn.isReceiver) {
            MBillMmpDaily.findByContractId(contractId)
          } else {
            Nil
          }
          (mbc, txns, myMbmds)
        }
    }
    billInfoOpt match {
      case Some((mbc, txns, mbmds)) =>
        otherRcvrsFut.map { otherRcvrs =>
          Ok(showAdnNodeBillingTpl(request.adnNode, mbmds, txns, mbc, otherRcvrs))
        }

      case None =>
        warn(s"showAdnNodeBilling($adnId): No active contracts found for node, but billing page requested by user ${request.pwOpt} ref=${request.headers.get("Referer")}")
        http404AdHoc
    }
  }


  /**
   * Инлайновый рендер сеток тарифных планов.
   * @param adnId id узла, к которому нужно найти и отрендерить посуточные тарифные сетки.
   * @return inline выхлоп для отображения внутри какой-то страницы с тарифами.
   */
  def _renderNodeMbmds(adnId: String) = IsAuth.async { implicit request =>
    val adnNodeFut = MAdnNodeCache.getByIdCached(adnId)
    val mbdms = DB.withConnection { implicit c =>
      // TODO Opt Нам тут нужны только номера договоров (id), а не сами договоры.
      val contracts = MBillContract.findForAdn(adnId, isActive = Some(true))
      val contractIds = contracts.map(_.id.get)
      MBillMmpDaily.findByContractIds(contractIds)
    }
    adnNodeFut.map {
      case Some(adnNode) =>
        Ok(_dailyMmpTariffPlansTpl(mbdms, adnNode))

      case None => http404AdHoc
    }
  }

}
