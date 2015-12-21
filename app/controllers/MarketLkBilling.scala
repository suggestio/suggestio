package controllers

import com.google.inject.Inject
import io.suggest.bill.TxnsListConstants
import models._
import models.jsm.init.MTargets
import models.mbill.{MContract, MDailyMmpsTplArgs, MTariffDaily, MTxn}
import models.mproj.ICommonDi
import models.req.IReq
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
  override val mCommonDi          : ICommonDi
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
  def paymentRequsites(adnId: String) = IsAdnNodeAdmin(adnId, U.Lk).async { implicit request =>
    val mbcsFut = Future {
      db.withConnection { implicit c =>
        MContract.findForAdn(adnId, isActive = Some(true))
      }
    }(AsyncUtil.jdbcExecutionContext)

    mbcsFut.flatMap { mbcs =>
      mbcs.headOption match {
        case Some(mbc) =>
          request.user.lkCtxData.map { implicit ctxData =>
            Ok(billPaymentBankTpl(request.mnode, mbc))
          }

        case None =>
          // Нет заключенных договоров, оплата невозможна.
          errorHandler.http404Fut
      }
    }
  }


  /**
   * Рендер страницы, содержащей общую биллинговую информацию для узла.
   * @param adnId id узла.
   */
  def showAdnNodeBilling(adnId: String) = IsAdnNodeAdmin(adnId, U.Lk).async { implicit request =>

    val billInfoOptFut = Future {
      db.withConnection { implicit c =>
        MContract.findForAdn(adnId, isActive = Some(true))
          .headOption
          .map { mbc =>
            val contractId = mbc.id.get
            // Если этот узел - приёмник рекламы, то нужно найти в базе его тарифные планы.
            val myMbmds = if (request.mnode.extras.adn.exists(_.isReceiver)) {
              MTariffDaily.findByContractId(contractId)
            } else {
              Nil
            }
            (mbc, myMbmds)
          }
      }
    }(AsyncUtil.jdbcExecutionContext)

    billInfoOptFut flatMap {
      case Some((mbc, mbmds)) =>
        request.user.lkCtxData.map { implicit ctxData =>
          val html = showAdnNodeBillingTpl(request.mnode, mbmds, mbc)
          Ok(html)
        }

      case None =>
        warn(s"showAdnNodeBilling($adnId): No active contracts found for node, but billing page requested by user ${request.user.personIdOpt} ref=${request.headers.get("Referer")}")
        errorHandler.http404Fut
    }
  }


  /** Подгрузка страницы из списка транзакций. */
  def txnsList(adnId: String, page: Int, inline: Boolean) = IsAdnNodeAdmin(adnId, U.Lk).async { implicit request =>
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
      ctxData0  <- request.user.lkCtxData
      txns      <- txnsFut
    } yield {
      implicit val ctxData = ctxData0.copy(
        jsiTgs = Seq(MTargets.BillTxnsList)
      )
      val render: Html = if (inline) {
        _txnsListTpl(txns)
      } else {
        txnsPageTpl(request.mnode, txns, currPage = page, txnsPerPage = tpp)
      }
      Ok(render)
        .withHeaders(
          TxnsListConstants.HAS_MORE_TXNS_HTTP_HDR -> (txns.size >= tpp).toString
        )
    }
  }


  /**
   * Одинаковые куски [[_renderNodeMbmdsWindow()]] вынесены в эту функцию.
   * Она собирает данные для рендера шаблонов, относящихся к этим экшенам и дергает фунцию рендера, когда всё готово.
   * @param mnode Текущий узел N2.
   * @return Фьючерс с Some и аргументами рендера. Если нет узла, то None
   */
  private def _prepareNodeMbmds(mnode: MNode)(implicit request: IReq[_]): Future[MDailyMmpsTplArgs] = {
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
