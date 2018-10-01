package controllers

import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import controllers.cbill._
import io.suggest.adv.info.{MNodeAdvInfo, MNodeAdvInfo4Ad}
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.order.MOrders
import io.suggest.media.{MMediaInfo, MMediaTypes}
import io.suggest.model.n2.node.MNode
import io.suggest.pick.PickleUtil
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mbill._
import models.mcal.MCalendars
import models.mctx.Context
import models.mproj.ICommonDi
import util.TplDataFormatUtil
import util.acl._
import util.ad.JdAdUtil
import util.adn.NodesUtil
import util.adv.AdvUtil
import util.adv.geo.AdvGeoRcvrsUtil
import util.billing.{Bill2Util, TfDailyUtil}
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
@Singleton
class LkBill2 @Inject() (
                          tfDailyUtil                 : TfDailyUtil,
                          mCalendars                  : MCalendars,
                          galleryUtil                 : GalleryUtil,
                          advUtil                     : AdvUtil,
                          override val reqUtil        : ReqUtil,
                          override val isAuth         : IsAuth,
                          nodesUtil                   : NodesUtil,
                          override val canViewOrder   : CanViewOrder,
                          override val canAccessItem  : CanAccessItem,
                          canViewNodeAdvInfo          : CanViewNodeAdvInfo,
                          override val isNodeAdmin    : IsNodeAdmin,
                          override val advGeoRcvrsUtil: AdvGeoRcvrsUtil,
                          override val jdAdUtil       : JdAdUtil,
                          override val mItems         : MItems,
                          override val bill2Util      : Bill2Util,
                          override val mOrders        : MOrders,
                          override val mCommonDi      : ICommonDi
                        )
  extends SioControllerImpl
  with MacroLogsImpl
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


  /** Возврат данных для рендера данных по размещению на узле..
    *
    * @param nodeId id узла.
    * @return Бинарь с публичной инфой по узлу, на котором планируется размещение.
    */
  def nodeAdvInfo(nodeId: String, forAdId: Option[String]) = canViewNodeAdvInfo(nodeId, forAdId).async { implicit request =>

    val galleryImgsFut = galleryUtil.galleryImgs(request.mnode)

    val mediaHostsMapFut = galleryImgsFut.flatMap { galleryImgs =>
      nodesUtil.nodeMediaHostsMap(
        gallery = galleryImgs
      )
    }

    implicit val ctx = implicitly[Context]

    val galleryCallsFut = galleryImgsFut.flatMap { galleryImgs =>
      galleryUtil.renderGalleryCdn(galleryImgs, mediaHostsMapFut)(ctx)
    }

    // Собрать картинки
    val galleryFut = for {
      galleryCalls <- galleryCallsFut
    } yield {
      for (galleryCall <- galleryCalls) yield {
        MMediaInfo(
          giType  = MMediaTypes.Image,
          url     = galleryCall.url,
          // thumb'ы: Не отображаются на экране из-за особенностей вёрстки; в дизайне не предусмотрены.
          thumb   = None /*Some(
            MMediaInfo(
              giType = MMediaTypes.Image,
              url    = dynImgUtil.thumb256Call(mimg, fillArea = true).url
            )
          )*/
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
          tfInfo.clauses.mapValues { p0 =>
            val p2 = p0 * bmc
            TplDataFormatUtil.setFormatPrice( p2 )(ctx)
          }
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
        nodeNameBasic   = request.mnode.meta.basic.name,
        nodeName        = request.mnode.guessDisplayNameOrIdOrQuestions,
        tfDaily         = Some(tfInfo),
        tfDaily4Ad      = tfDaily4Ad,
        meta            = request.mnode.meta.public,
        gallery         = gallery
      )

      // Сериализовать и отправить ответ.
      val bbuf = PickleUtil.pickle(m)
      Ok( ByteString(bbuf) )
    }
  }

}
