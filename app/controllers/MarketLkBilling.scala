package controllers

import com.google.inject.Inject
import io.suggest.bill.TxnsListConstants
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models._
import models.jsm.init.MTargets
import models.mbill.{MContract, MDailyMmpsTplArgs, MTariffDaily, MTxn}
import models.mproj.MCommonDi
import models.req.ISioReq
import org.elasticsearch.search.sort.SortOrder
import play.twirl.api.Html
import util.PlayMacroLogsImpl
import util.acl._
import util.async.AsyncUtil
import util.img.GalleryUtil
import views.html.lk.billing._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:23
 * Description: Контроллер управления биллингом в личном кабинете узла рекламной сети.
 */
class MarketLkBilling @Inject() (
  galleryUtil                     : GalleryUtil,
  override val mCommonDi          : MCommonDi
)
  extends SioController
  with PlayMacroLogsImpl
  with IsAdnNodeAdmin
  with IsAuth
  with IsAuthNode
{

  import LOGGER._
  import mCommonDi._

  private val TXNS_PER_PAGE: Int = configuration.getInt("market.billing.txns.page.size") getOrElse 10


  /** Отобразить какую-то страницу с реквизитами для платежа. */
  def paymentRequsites(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val mbcsFut = Future {
      db.withConnection { implicit c =>
        MContract.findForAdn(adnId, isActive = Some(true))
      }
    }(AsyncUtil.jdbcExecutionContext)
    for (mbcs <- mbcsFut) yield {
      mbcs.headOption match {
        case Some(mbc) =>
          Ok(billPaymentBankTpl(request.adnNode, mbc))

        case None =>
          // Нет заключенных договоров, оплата невозможна.
          errorHandler.http404ctx
      }
    }
  }


  /**
   * Рендер страницы, содержащей общую биллинговую информацию для узла.
   * @param adnId id узла.
   */
  def showAdnNodeBilling(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val isProducer = request.adnNode
      .extras
      .adn
      .exists(_.isProducer)
    val otherRcvrsFut: Future[Seq[MNode]] = if (isProducer) {
      val msearch = new MNodeSearchDfltImpl {
        override def limit          = 100
        override def withAdnRights  = Seq(AdnRights.RECEIVER)
        override def withoutIds     = Seq(adnId)
        override def withNameSort   = Some( SortOrder.ASC )
        override def nodeTypes      = Seq( MNodeTypes.AdnNode )
      }
      MNode.dynSearch( msearch )

    } else {
      Future successful Nil
    }
    val billInfoOpt = db.withConnection { implicit c =>
      MContract.findForAdn(adnId, isActive = Some(true))
        .headOption
        .map { mbc =>
          val contractId = mbc.id.get
          // Если этот узел - приёмник рекламы, то нужно найти в базе его тарифные планы.
          val myMbmds = if (request.adnNode.extras.adn.exists(_.isReceiver)) {
            MTariffDaily.findByContractId(contractId)
          } else {
            Nil
          }
          val allRcvrAdnIds = if (isProducer) {
            MTariffDaily.findAllAdnIds
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
        errorHandler.http404ctx
    }
  }


  /** Подгрузка страницы из списка транзакций. */
  def txnsList(adnId: String, page: Int, inline: Boolean) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val tpp = TXNS_PER_PAGE
    val offset = page * tpp

    val txnsFut = Future {
      db.withConnection { implicit c =>
        val mbcs = MContract.findForAdn(adnId, isActive = None)
        val mbcIds = mbcs.flatMap(_.id).toSet
        MTxn.findForContracts(mbcIds, limit = tpp, offset = offset)
      }
    }(AsyncUtil.jdbcExecutionContext)

    for {
      txns <- txnsFut
    } yield {
      implicit val jsInitTgs = Seq(MTargets.BillTxnsList)
      val render: Html = if (inline) {
        _txnsListTpl(txns)
      } else {
        txnsPageTpl(request.adnNode, txns, currPage = page, txnsPerPage = tpp)
      }
      Ok(render)
        .withHeaders(
          TxnsListConstants.HAS_MORE_TXNS_HTTP_HDR -> (txns.size >= tpp).toString
        )
    }
  }


  /**
   * Одинаковые куски [[_renderNodeMbmds()]] и [[_renderNodeMbmdsWindow()]] вынесены в эту функцию.
   * Она собирает данные для рендера шаблонов, относящихся к этим экшенам и дергает фунцию рендера, когда всё готово.
   * @param mnode Текущий узел N2.
   * @return Фьючерс с Some и аргументами рендера. Если нет узла, то None
   */
  private def _prepareNodeMbmds(mnode: MNode)(implicit request: ISioReq[_]): Future[MDailyMmpsTplArgs] = {
    // TODO По идее надо бы проверять узел на то, является ли он ресивером наверное?
    val nodeId = mnode.id.get

    val tariffsFut = Future {
      db.withConnection { implicit c =>
        // TODO Opt Нам тут нужны только номера договоров (id), а не сами договоры.
        val contracts = MContract.findForAdn(nodeId, isActive = Some(true))
        val contractIds = contracts.map(_.id.get)
        MTariffDaily.findByContractIds(contractIds)
      }
    }(AsyncUtil.jdbcExecutionContext)

    val galleryFut = galleryUtil.galleryImgs(mnode)

    for {
      tariffs   <- tariffsFut
      gallery   <- galleryFut
    } yield {
      MDailyMmpsTplArgs(
        tariffs = tariffs,
        mnode = mnode,
        gallery = gallery
      )
    }
  }


  /**
   * Инлайновый рендер сеток тарифных планов.
   * @param adnId id узла, к которому нужно найти и отрендерить посуточные тарифные сетки.
   * @return inline выхлоп для отображения внутри какой-то страницы с тарифами.
   */
  def _renderNodeMbmds(adnId: String) = IsAuthNode(adnId).async { implicit request =>
    _prepareNodeMbmds(request.mnode) map { args =>
      Ok( _dailyMmpTariffPlansTpl(args) )
    }
  }


  /**
   * Тоже самое, что и _renderNodeMbmds(), но ещё обрамляет всё дело в окно, пригодное для отображения юзеру
   * в плавающей форме.
   * @param adnId id просматриваемого узла.
   */
  def _renderNodeMbmdsWindow(adnId: String) = IsAuthNode(adnId).async { implicit request =>
    _prepareNodeMbmds(request.mnode) map { args =>
      Ok( _dailyMmpsWindowTpl(args) )
    }
  }

}
