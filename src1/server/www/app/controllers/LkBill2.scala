package controllers

import com.google.inject.Inject
import com.google.inject.name.Named
import controllers.cbill.{LkBill2Cart, LkBillTxns}
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.txn.MTxns
import io.suggest.model.common.OptId
import io.suggest.util.logs.MacroLogsImpl
import models.MNode
import models.im.make.IMaker
import models.mbill.{MDailyTfTplArgs, MLkBillNodeTplArgs, MRcvrInfoTplArgs, MShowOrderTplArgs}
import models.mcal.MCalendars
import models.mproj.ICommonDi
import util.acl.{CanAccessItem, CanViewOrder, IsAdnNodeAdmin, IsAuthNode}
import util.billing.{Bill2Util, TfDailyUtil}
import util.img.GalleryUtil
import views.html.lk.billing._
import views.html.lk.billing.pay._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 19:17
  * Description: Контроллер биллинга второго поколения в личном кабинете.
  * Прошлый контроллер назывался MarketLkBilling.
  */
class LkBill2 @Inject() (
  tfDailyUtil                 : TfDailyUtil,
  mCalendars                  : MCalendars,
  galleryUtil                 : GalleryUtil,
  canViewOrder                : CanViewOrder,
  override val canAccessItem  : CanAccessItem,
  @Named("blk") override val blkImgMaker  : IMaker,
  isAuthNode                  : IsAuthNode,
  override val isAdnNodeAdmin : IsAdnNodeAdmin,
  override val mItems         : MItems,
  override val bill2Util      : Bill2Util,
  override val mTxns          : MTxns,
  override val mCommonDi      : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
  with LkBill2Cart
  with LkBillTxns
{

  import mCommonDi._

  private def _dailyTfArgsFut(mnode: MNode): Future[Option[MDailyTfTplArgs]] = {
    if (mnode.extras.adn.exists(_.isReceiver)) {
      for {
        // Получить данные по тарифу.
        dailyTf   <- tfDailyUtil.forcedNodeTf( mnode )
        // Прочитать календари, относящиеся к тарифу.
        calsMap   <- mCalendars.multiGetMap( dailyTf.calIds )
      } yield {
        val args1 = MDailyTfTplArgs(
          mnode   = mnode,
          dailyTf = dailyTf,
          calsMap = calsMap
        )
        Some(args1)
      }

    } else {
      Future.successful(None)
    }
  }

  /**
    * Рендер страницы с биллингом текущего узла и юзера.
    * Биллинг сместился в сторону юзера, поэтому тут тоже всё размыто.
    *
    * @param nodeId id узла.
    * @return 200 Ок со страницей биллинга узла.
    */
  def onNode(nodeId: String) = isAdnNodeAdmin.Get(nodeId, U.Lk).async { implicit request =>
    val dailyTfArgsFut = _dailyTfArgsFut(request.mnode)

    // Отрендерить результаты, когда всё получено:
    for {
      lkCtxData   <- request.user.lkCtxDataFut
      dailyTfArgs <- dailyTfArgsFut
    } yield {

      val args = MLkBillNodeTplArgs(
        mnode       = request.mnode,
        dailyTfArgs = dailyTfArgs
      )

      implicit val ctxData = lkCtxData
      Ok( nodeBillingTpl(args) )
    }
  }

  /**
    * Страница "спасибо за покупку". Финиш.
    *
    * @param onNodeId В рамках ЛК какой ноды происходит движуха.
    * @return Страница "спасибо за покупку".
    */
  def thanksForBuy(onNodeId: String) = isAdnNodeAdmin.Get(onNodeId, U.Lk).async { implicit request =>
    request.user.lkCtxDataFut.flatMap { implicit ctxData =>
      Ok(ThanksForBuyTpl(request.mnode))
    }
  }


  /** Рендер окошка с по целевому узлу-ресиверу.
    *
    * @param nodeId id узла-ресивера.
    * @return 200 Ок с версткой окошка.
    *         404 если узел не найден или не является ресивером.
    */
  def _rcvrInfoWnd(nodeId: String) = isAuthNode(nodeId).async { implicit request =>
    val dailyTfArgsOptFut = _dailyTfArgsFut(request.mnode)
    val galleryFut = galleryUtil.galleryImgs(request.mnode)

    val okFut = for {
      dailyTfArgsOpt  <- dailyTfArgsOptFut
      dailyTfArgs     =  dailyTfArgsOpt.get
      gallery         <- galleryFut
    } yield {
      val args = MRcvrInfoTplArgs(
        mnode   = request.mnode,
        dailyTf = dailyTfArgs.dailyTf,
        calsMap = dailyTfArgs.calsMap,
        gallery = gallery
      )
      Ok( _rcvrInfoWndTpl(args) )
    }

    okFut.recover { case _: NoSuchElementException =>
      NotFound("Not a receiver: " + nodeId)
    }
  }


  /** Показать страничку с заказом.
    *
    * @param orderId id ордера.
    * @param onNodeId id узла, на котором открыта морда ЛК.
    */
  def showOrder(orderId: Gid_t, onNodeId: MEsUuId) = canViewOrder.Get(orderId, onNodeId, U.Lk).async { implicit request =>
    // Поискать транзакцию по оплате ордера, если есть.
    val txnsFut = slick.db.run {
      bill2Util.getOrderTxns(orderId)
    }

    // Посчитать текущую стоимость заказа:
    val orderPricesFut = slick.db.run {
      bill2Util.getOrderPrices(orderId)
    }

    // Собрать карту балансов по id. Она нужна для рендера валюты транзакции. Возможно ещё для чего-либо пригодится.
    // Нет смысла цеплять это об необязательно найденную транзакцию, т.к. U.Lk наверху гарантирует, что mBalancesFut уже запущен на исполнение.
    val mBalsMapFut = for {
      mBals <- request.user.mBalancesFut
    } yield {
      OptId.els2idMap[Gid_t, MBalance](mBals)
    }

    // Отрендерить ответ, когда всё будет готово.
    for {
      txns          <- txnsFut
      orderPrices   <- orderPricesFut
      mBalsMap      <- mBalsMapFut
    } yield {
      val tplArgs = MShowOrderTplArgs(
        mnode         = request.mnode,
        morder        = request.morder,
        orderPrices   = orderPrices,
        txns          = txns,
        balances      = mBalsMap
      )
      Ok( ShowOrderTpl(tplArgs) )
    }
  }

}
