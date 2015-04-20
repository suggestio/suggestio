package controllers

import com.google.inject.Inject
import play.api.i18n.MessagesApi
import util.PlayMacroLogsImpl
import util.acl._
import models._
import util.xplay.SioHttpErrorHandler
import views.html.lk.billing._
import play.api.db.Database
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import play.api.mvc.Result
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:23
 * Description: Контроллер управления биллингом в личном кабинете узла рекламной сети.
 */
class MarketLkBilling @Inject() (
  override val messagesApi: MessagesApi,
  db: Database
)
  extends SioController with PlayMacroLogsImpl
{

  import LOGGER._

  val TXNS_PER_PAGE: Int = configuration.getInt("market.billing.txns.page.size") getOrElse 10


  /** Отобразить какую-то страницу с реквизитами для платежа. */
  def paymentRequsites(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    val mbcs = db.withConnection { implicit c =>
      MBillContract.findForAdn(adnId, isActive = Some(true))
    }
    mbcs.headOption match {
      case Some(mbc) =>
        Ok(billPaymentBankTpl(request.adnNode, mbc))

      case None =>
        // Нет заключенных договоров, оплата невозможна.
        SioHttpErrorHandler.http404ctx
    }
  }


  /**
   * Рендер страницы, содержащей общую биллинговую информацию для узла.
   * @param adnId id узла.
   */
  def showAdnNodeBilling(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val isProducer = request.adnNode.adn.isProducer
    val otherRcvrsFut = if (isProducer) {
      MAdnNode.findByAllAdnRights(Seq(AdnRights.RECEIVER), withoutTestNodes = false)
        .map { _.filter(_.id.get != adnId).sortBy(_.meta.name) }
    } else {
      Future successful Nil
    }
    val billInfoOpt = db.withConnection { implicit c =>
      MBillContract.findForAdn(adnId, isActive = Some(true))
        .headOption
        .map { mbc =>
          val contractId = mbc.id.get
          // Если этот узел - приёмник рекламы, то нужно найти в базе его тарифные планы.
          val myMbmds = if (request.adnNode.adn.isReceiver) {
            MBillMmpDaily.findByContractId(contractId)
          } else {
            Nil
          }
          val allRcvrAdnIds = if (isProducer) {
            MBillMmpDaily.findAllAdnIds
          } else {
            Nil
          }
          (mbc, myMbmds, allRcvrAdnIds)
        }
    }
    billInfoOpt match {
      case Some((mbc, mbmds, allRcvrAdnIds)) =>
        val allRcvrAdnIdsSet = allRcvrAdnIds.toSet
        otherRcvrsFut.map { otherRcvrs =>
          val otherRcvrs1 = otherRcvrs.filter(_.id.exists(allRcvrAdnIdsSet.contains))
          Ok(showAdnNodeBillingTpl(request.adnNode, mbmds, mbc, otherRcvrs1))
        }

      case None =>
        warn(s"showAdnNodeBilling($adnId): No active contracts found for node, but billing page requested by user ${request.pwOpt} ref=${request.headers.get("Referer")}")
        SioHttpErrorHandler.http404ctx
    }
  }


  /** Подгрузка страницы из списка транзакций. */
  def _txnsList(adnId: String, page: Int) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    val tpp = TXNS_PER_PAGE
    val offset = page * tpp
    val txns = db.withConnection { implicit c =>
      val mbcs = MBillContract.findForAdn(adnId, isActive = None)
      val mbcIds = mbcs.flatMap(_.id).toSet
      MBillTxn.findForContracts(mbcIds, limit = tpp, offset = offset)
    }
    Ok(_txnsPageTpl(request.adnNode, txns, currPage = page, txnsPerPage = tpp))
  }


  /**
   * Одинаковые куски [[_renderNodeMbmds()]] и [[_renderNodeMbmdsWindow()]] вынесены в эту функцию.
   * Она собирает данные для рендера шаблонов, относящихся к этим экшенам и дергает фунцию рендера, когда всё готово.
   * @param adnId id узла.
   * @param f функция вызова рендера результата.
   * @return Фьючерс с результатом. Если нет узла, то 404.
   */
  private def _prepareNodeMbmds(adnId: String)(f: (List[MBillMmpDaily], MAdnNode) => Result)
                               (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // TODO По идее надо бы проверять узел на то, является ли он ресивером наверное?
    val adnNodeFut = MAdnNodeCache.getById(adnId)
    val mbdms = db.withConnection { implicit c =>
      // TODO Opt Нам тут нужны только номера договоров (id), а не сами договоры.
      val contracts = MBillContract.findForAdn(adnId, isActive = Some(true))
      val contractIds = contracts.map(_.id.get)
      MBillMmpDaily.findByContractIds(contractIds)
    }
    adnNodeFut.map {
      case Some(adnNode) =>
        f(mbdms, adnNode)
      case None =>
        SioHttpErrorHandler.http404ctx
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
   * Тоже самое, что и _renderNodeMbmds(), но ещё обрамляет всё дело в окно, пригодное для отображения юзеру
   * в плавающей форме.
   * @param adnId id просматриваемого узла.
   */
  def _renderNodeMbmdsWindow(adnId: String) = IsAuth.async { implicit request =>
    _prepareNodeMbmds(adnId) { (mbdms, adnNode) =>
      Ok(_dailyMmpsWindowTpl(mbdms, adnNode))
    }
  }

}
