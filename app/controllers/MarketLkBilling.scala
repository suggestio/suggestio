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
import play.api.mvc.{RequestHeader, Result}

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
    val isReceiver = request.adnNode.adn.isReceiver
    val showOtherRcvrs = isProducer
    val otherRcvrsFut = if (showOtherRcvrs) {
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
          val myMbmds = if (isReceiver) {
            MBillMmpDaily.findByContractId(contractId)
          } else {
            Nil
          }
          val allRcvrAdnIds = if (showOtherRcvrs) {
            MBillMmpDaily.findAllAdnIds
          } else {
            Nil
          }
          (mbc, txns, myMbmds, allRcvrAdnIds)
        }
    }
    billInfoOpt match {
      case Some((mbc, txns, mbmds, allRcvrAdnIds)) =>
        val allRcvrAdnIdsSet = allRcvrAdnIds.toSet
        otherRcvrsFut.map { otherRcvrs =>
          val otherRcvrs1 = otherRcvrs.filter(_.id.exists(allRcvrAdnIdsSet.contains))
          Ok(showAdnNodeBillingTpl(request.adnNode, mbmds, txns, mbc, otherRcvrs1))
        }

      case None =>
        warn(s"showAdnNodeBilling($adnId): No active contracts found for node, but billing page requested by user ${request.pwOpt} ref=${request.headers.get("Referer")}")
        http404AdHoc
    }
  }


  /**
   * Одинаковые куски [[_renderNodeMbmds()]] и [[_renderNodeMbmdsWindow()]] вынесены в эту функцию.
   * Она собирает данные для рендера шаблонов, относящихся к этим экшенам и дергает фунцию рендера, когда всё готово.
   * @param adnId id узла.
   * @param f функция вызова рендера результата.
   * @return Фьючерс с результатом. Если нет узла, то 404.
   */
  private def _prepareNodeMbmds(adnId: String)(f: (List[MBillMmpDaily], MAdnNode) => Result)(implicit request: RequestHeader): Future[Result] = {
    // TODO По идее надо бы проверять узел на то, является ли он ресивером наверное?
    val adnNodeFut = MAdnNodeCache.getByIdCached(adnId)
    val mbdms = DB.withConnection { implicit c =>
      // TODO Opt Нам тут нужны только номера договоров (id), а не сами договоры.
      val contracts = MBillContract.findForAdn(adnId, isActive = Some(true))
      val contractIds = contracts.map(_.id.get)
      MBillMmpDaily.findByContractIds(contractIds)
    }
    adnNodeFut.map {
      case Some(adnNode) =>
        f(mbdms, adnNode)

      case None => http404AdHoc
    }
  }

  /**
   * Инлайновый рендер сеток тарифных планов.
   * @param adnId id узла, к которому нужно найти и отрендерить посуточные тарифные сетки.
   * @return inline выхлоп для отображения внутри какой-то страницы с тарифами.
   */
  def _renderNodeMbmds(adnId: String) = IsAuth.async { implicit request =>
    _prepareNodeMbmds(adnId) { (mbdms, adnNode) =>
      Ok(_dailyMmpTariffPlansTpl(mbdms, adnNode))
    }
  }


  /**
   * Тоже самое, что и [[_renderNodeMbmds()]], но ещё обрамляет всё дело в окно, пригодное для отображения юзеру
   * в плавающей форме.
   * @param adnId id просматриваемого узла.
   */
  def _renderNodeMbmdsWindow(adnId: String) = IsAuth.async { implicit request =>
    _prepareNodeMbmds(adnId) { (mbdms, adnNode) =>
      Ok(_dailyMmpsWindowTpl(mbdms, adnNode))
    }
  }

}
