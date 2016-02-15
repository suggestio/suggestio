package controllers

import com.google.inject.Inject
import controllers.cbill.{LkBillTxns, LkBill2Cart}
import io.suggest.mbill2.m.txn.MTxns
import models.MNode
import models.mbill.{MRcvrInfoTplArgs, MDailyTfTplArgs, MLkBillNodeTplArgs}
import models.mcal.MCalendars
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.acl.IsAuthNode
import util.billing.{TfDailyUtil, Bill2Util}
import util.img.GalleryUtil
import views.html.lk.billing._

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
  override val bill2Util      : Bill2Util,
  override val mTxns          : MTxns,
  override val mCommonDi      : ICommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with LkBill2Cart
  with LkBillTxns
  with IsAuthNode
{

  import mCommonDi._

  private def _dailyTfArgsFut(mnode: MNode): Future[Option[MDailyTfTplArgs]] = {
    if (mnode.extras.adn.exists(_.isReceiver)) {
      for {
        // Получить данные по тарифу.
        dailyTf   <- tfDailyUtil.nodeTf( mnode )
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
  def onNode(nodeId: String) = IsAdnNodeAdmin(nodeId, U.Lk).async { implicit request =>
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
  def thanksForBuy(onNodeId: String) = IsAdnNodeAdmin(onNodeId, U.Lk).async { implicit request =>
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
  def _rcvrInfoWnd(nodeId: String) = IsAuthNode(nodeId).async { implicit request =>
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

}
