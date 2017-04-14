package controllers

import akka.util.ByteString
import com.google.inject.Inject
import com.google.inject.name.Named
import controllers.cbill._
import io.suggest.adv.info.{MNodeAdvInfo, MNodeAdvInfo4Ad}
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.order.MOrders
import io.suggest.mbill2.m.txn.MTxns
import io.suggest.media.{MMediaInfo, MMediaTypes}
import io.suggest.pick.PickleUtil
import io.suggest.util.logs.MacroLogsImpl
import models.MNode
import models.im.make.IMaker
import models.mbill._
import models.mcal.MCalendars
import models.mctx.Context
import models.mproj.ICommonDi
import play.twirl.api.{Html, Template2}
import util.acl._
import util.adv.AdvUtil
import util.billing.{Bill2Util, TfDailyUtil}
import util.img.{DynImgUtil, GalleryUtil, LogoUtil}
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
                          advUtil                     : AdvUtil,
                          override val logoUtil       : LogoUtil,
                          override val canViewOrder   : CanViewOrder,
                          override val canAccessItem  : CanAccessItem,
                          @Named("blk") override val blkImgMaker  : IMaker,
                          canViewNodeAdvInfo          : CanViewNodeAdvInfo,
                          override val isNodeAdmin    : IsNodeAdmin,
                          dynImgUtil                  : DynImgUtil,
                          override val mItems         : MItems,
                          override val bill2Util      : Bill2Util,
                          override val mTxns          : MTxns,
                          override val mOrders        : MOrders,
                          override val mCommonDi      : ICommonDi
                        )
  extends SioControllerImpl
  with MacroLogsImpl
  with LkBillTxns
  with LkBillOrders
{

  import mCommonDi._

  private def _dailyTfArgsFut(mnode: MNode, madOpt: Option[MNode] = None): Future[Option[MTfDailyTplArgs]] = {
    if (mnode.extras.adn.exists(_.isReceiver)) {
      for {
        // Получить данные по тарифу.
        tfDaily   <- tfDailyUtil.nodeTf( mnode )
        // Прочитать календари, относящиеся к тарифу.
        calsMap   <- mCalendars.multiGetMap( tfDaily.calIds )
      } yield {

        // Подготовить инфу по ценам на карточку, если она задана.
        val madTfOpt = for (mad <- madOpt) yield {
          val bmc = advUtil.getAdModulesCount( mad )
          val madTf = tfDaily.withClauses(
            tfDaily.clauses.mapValues { mdc =>
              mdc.withAmount(
                mdc.amount * bmc
              )
            }
          )
          MAdTfInfo(bmc, madTf)
        }

        val args1 = MTfDailyTplArgs(
          mnode     = mnode,
          tfDaily   = tfDaily,
          madTfOpt  = madTfOpt,
          calsMap   = calsMap
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
  def onNode(nodeId: String) = csrf.AddToken {
    isNodeAdmin(nodeId, U.Lk).async { implicit request =>
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
  }

  /**
    * Страница "спасибо за покупку". Финиш.
    *
    * @param onNodeId В рамках ЛК какой ноды происходит движуха.
    * @return Страница "спасибо за покупку".
    */
  def thanksForBuy(onNodeId: String) = csrf.AddToken {
    isNodeAdmin(onNodeId, U.Lk).async { implicit request =>
      request.user.lkCtxDataFut.map { implicit ctxData =>
        Ok(ThanksForBuyTpl(request.mnode))
      }
    }
  }


  /** Рендер окошка с по целевому узлу-ресиверу.
    *
    * @param nodeId id узла-ресивера.
    * @return 200 Ок с версткой окошка.
    *         404 если узел не найден или не является ресивером.
    */
  def _rcvrInfoWnd(nodeId: String) = _rcvrInfoResp(_rcvrInfoWndTpl, nodeId)

  /** Рендер только наполнения окошка по целевому узлу-ресиверу. */
  def _rcvrInfoWndBody(nodeId: String, forAdId: Option[String]) = _rcvrInfoResp(_rcvrInfoPopBodyTpl, nodeId, forAdId)

  private def _rcvrInfoResp(
                             tpl      : Template2[IRcvrInfoTplArgs, Context, Html],
                             nodeId   : String,
                             forAdId  : Option[String] = None
                           ) = {
    canViewNodeAdvInfo(nodeId, forAdId).async { implicit request =>
      val madOpt = request.adProdReqOpt
        .map(_.mad)
      val dailyTfArgsOptFut = _dailyTfArgsFut(request.mnode, madOpt)
      val galleryFut = galleryUtil.galleryImgs(request.mnode)

      val okFut = for {
        dailyTfArgsOpt  <- dailyTfArgsOptFut
        dailyTfArgs     =  dailyTfArgsOpt.get
        gallery         <- galleryFut
      } yield {
        val args = MRcvrInfoTplArgs(
          tfArgs  = dailyTfArgs,
          gallery = gallery
        )
        Ok( tpl.render(args, implicitly[Context]) )
      }

      okFut.recover { case _: NoSuchElementException =>
        NotFound("Not a receiver: " + nodeId)
      }
    }
  }


  /** Возврат данных для рендера данных по размещению на узле..
    *
    * @param nodeId id узла.
    * @return Бинарь с публичной инфой по узлу, на котором планируется размещение.
    */
  // TODO Не используется, т.к. потом было решено ускорить вёрстку: вместо чистовой react-вёрстки использовать существующую html string + innerHtml.
  private def nodeAdvInfo(nodeId: String, forAdId: Option[String]) = canViewNodeAdvInfo(nodeId, forAdId).async { implicit request =>
    implicit val ctx = implicitly[Context]

    // Собрать картинки
    val galleryFut = for {
      gal <- galleryUtil.galleryImgs(request.mnode)
    } yield {
      for (mimg <- gal) yield {
        MMediaInfo(
          giType  = MMediaTypes.Image,
          url     = galleryUtil.dynLkBigCall(mimg)(ctx).url,
          thumb   = Some(
            MMediaInfo(
              giType = MMediaTypes.Image,
              url    = dynImgUtil.thumb256Call(mimg, fillArea = true).url
            )
          )
        )
      }
    }

    // Собрать данные по тарифу.
    val tfInfoFut = tfDailyUtil.getTfInfo( request.mnode )(ctx)

    // Подготовить в фоне данные по тарифу в контексте текущей карточки.
    val tfDaily4AdFut = FutureUtil.optFut2futOpt(request.adProdReqOpt) { adProdReq =>
      val bmc = advUtil.getAdModulesCount( adProdReq.mad )
      for {
        tfInfo <- tfInfoFut
      } yield {
        val tdDaily4ad = tfInfo.withClauses(
          tfInfo.clauses.mapValues(_ * bmc)
        )
        val r = MNodeAdvInfo4Ad(
          blockModulesCount = bmc,
          tfDaily           = tdDaily4ad
        )
        Some(r)
      }
    }

    // Собрать итоговый ответ клиенту:
    for {
      tfInfo      <- tfInfoFut
      gallery     <- galleryFut
      tfDaily4Ad  <- tfDaily4AdFut
    } yield {
      // Собрать финальный инстанс модели аргументов для рендера:
      val m = MNodeAdvInfo(
        nodeName    = request.mnode.guessDisplayNameOrIdOrQuestions,
        tfDaily     = Some(tfInfo),
        tfDaily4Ad  = tfDaily4Ad,
        meta        = request.mnode.meta.public,
        gallery     = gallery
      )

      // Сериализовать и отправить ответ.
      val bbuf = PickleUtil.pickle(m)
      Ok( ByteString(bbuf) )
    }
  }

}
