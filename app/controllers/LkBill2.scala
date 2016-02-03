package controllers

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.price.MPrice
import models.blk.{RenderArgs, IRenderArgs}
import models.im.make.Makers
import models.mctx.Context
import models.mlk.bill.{MCartItem, MCartTplArgs}
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.acl.IsAdnNodeAdmin
import util.billing.Bill2Util
import util.blocks.{BlocksConf, BgImg}
import views.html.lk.billing.cart._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 19:17
  * Description: Контроллер биллинга поколения биллинга в личном кабинете.
  */
class LkBill2 @Inject() (
  bill2Util                   : Bill2Util,
  override val mCommonDi      : ICommonDi
)
  extends SioControllerImpl
  with IsAdnNodeAdmin
  with PlayMacroLogsImpl
{

  import mCommonDi._

  private def ADS_SZ_MULT = 0.5F

  /**
    * Рендер страницы с корзиной покупок юзера в рамках личного кабинета узла.
    *
    * @param onNodeId На каком узле сейчас смотрим корзину текущего юзера?
    * @param r Куда производить возврат из корзины.
    * @return 200 ОК с html страницей корзины.
    */
  def cart(onNodeId: String, r: Option[String]) = IsAdnNodeAdmin(onNodeId, U.Lk, U.ContractId).async { implicit request =>

    // Узнать id контракта юзера. Сам контракт не важен.
    val mcIdOptFut = request.user.contractIdOptFut

    // Найти ордер-корзину юзера в базе биллинга:
    val cartOptFut = mcIdOptFut.flatMap { mcIdOpt =>
      FutureUtil.optFut2futOpt(mcIdOpt) { mcId =>
        bill2Util.getCart(mcId)
      }
    }

    // Найти item'ы корзины:
    val mitemsFut = cartOptFut.flatMap { cartOpt =>
      cartOpt
        .flatMap(_.id)
        .fold [Future[Seq[MItem]]] (Future.successful(Nil)) { bill2Util.orderItems }
    }

    // Собрать все карточки, относящиеся к mitem'ам:
    val madsFut = mitemsFut.flatMap { mitems =>
      val wantAdIds = mitems.iterator.map(_.adId).toSet
      mNodeCache.multiGet(wantAdIds)
    }

    // Параллельно собираем контекст рендера
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxData
    } yield {
      implicit val ctxData = lkCtxData
      implicitly[Context]
    }

    // Получаем мультипликатор размера отображения.
    val szMult = ADS_SZ_MULT

    // Собрать карту аргументов для рендера карточек
    val brArgsMapFut: Future[Map[String, IRenderArgs]] = for {
      ctx     <- ctxFut
      mads    <- madsFut
      brArgss <- {
        Future.traverse(mads) { mad =>
          for {
            bgOpt <- BgImg.maybeMakeBgImgWith(mad, Makers.Block, szMult, ctx.deviceScreenOpt)
          } yield {
            val ra = RenderArgs(
              mad           = mad,
              bc            = BlocksConf.applyOrDefault(mad),
              withEdit      = false,
              bgImg         = bgOpt,
              szMult        = szMult,
              inlineStyles  = true,
              isFocused     = false
            )
            mad.id.get -> ra
          }
        }
      }
    } yield {
      brArgss.toMap
    }

    // Сборка карты итемов ордера, сгруппированных по adId
    val mItemsMapByAdFut = for {
      mitems <- mitemsFut
    } yield {
      mitems.groupBy(_.adId)
    }

    // Сборка списка элементов корзины.
    val cartItemsFut: Future[Seq[MCartItem]] = for {
      mItemsMapByAd   <- mItemsMapByAdFut
      brArgsMap       <- brArgsMapFut
    } yield {
      mItemsMapByAd
        .iterator
        .map { case (madId, mitemsAd) =>
          MCartItem(mitemsAd, brArgsMap(madId))
        }
        .toSeq
    }

    // Рассчет общей стоимости корзины
    val totalPricesFut: Future[Map[String, MPrice]] = for {
      mitems <- mitemsFut
    } yield {
      mitems.iterator
        .map { _.price }
        .toSeq
        .groupBy(_.currency.getCurrencyCode)
        .mapValues { prices =>
          prices.head.copy(
            amount = prices.iterator.map(_.amount).sum
          )
        }
    }

    // Рендер и возврат ответа
    for {
      ctx           <- ctxFut
      cartItems     <- cartItemsFut
      totalPrices   <- totalPricesFut
    } yield {
      // Сборка аргументов для вызова шаблона
      val args = MCartTplArgs(
        mnode         = request.mnode,
        items         = cartItems,
        r             = r,
        totalPrices   = totalPrices
      )

      // Рендер результата
      val html = cartTpl(args)(ctx)
      Ok(html)
    }
  }

}
