package controllers.cbill

import controllers.{routes, SioController}
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.item.{ItemStatusChanged, MItem}
import io.suggest.mbill2.m.order.{OrderStatusChanged, IOrderWithItems}
import models.adv.tpl.MAdvPricing
import models.blk.{IRenderArgs, RenderArgs}
import models.im.make.Makers
import models.mctx.Context
import models.mlk.bill.{MCartItem, MCartTplArgs}
import util.PlayMacroLogsI
import util.acl.IsAdnNodeAdmin
import util.billing.IBill2UtilDi
import util.blocks.{BgImg, BlocksConf}
import views.html.lk.billing.cart._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 11:19
  * Description: Аддон для [[controllers.LkBill2]] для экшенов обработки корзины.
  */
trait LkBill2Cart
  extends SioController
  with IBill2UtilDi
  with PlayMacroLogsI
  with IsAdnNodeAdmin
{

  import mCommonDi._
  import dbConfig.driver.api.DBIO

  /** Отображение карточек в таком вот размере. */
  private def ADS_SZ_MULT = 0.25F

  /**
    * Рендер страницы с корзиной покупок юзера в рамках личного кабинета узла.
    *
    * @param onNodeId На каком узле сейчас смотрим корзину текущего юзера?
    * @param r Куда производить возврат из корзины.
    * @return 200 ОК с html страницей корзины.
    */
  def cart(onNodeId: String, r: Option[String]) = IsAdnNodeAdminGet(onNodeId, U.Lk, U.ContractId).async { implicit request =>

    // Узнать id контракта юзера. Сам контракт не важен.
    val mcIdOptFut = request.user.contractIdOptFut

    // Найти ордер-корзину юзера в базе биллинга:
    val cartOptFut = mcIdOptFut.flatMap { mcIdOpt =>
      FutureUtil.optFut2futOpt(mcIdOpt) { mcId =>
        bill2Util.getCartOrder(mcId)
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
    val totalPricingFut = for {
      mitems <- mitemsFut
    } yield {
      MAdvPricing(
        prices = bill2Util.items2pricesIter(mitems).toIterable,
        // true тут выставлено просто потому что в новом биллинге всегда доплачивается через внешнюю кассу.
        hasEnoughtMoney = true
      )
    }

    // Рендер и возврат ответа
    for {
      ctx           <- ctxFut
      cartItems     <- cartItemsFut
      totalPricing  <- totalPricingFut
    } yield {
      // Сборка аргументов для вызова шаблона
      val args = MCartTplArgs(
        mnode         = request.mnode,
        items         = cartItems,
        r             = r,
        totalPricing  = totalPricing
      )

      // Рендер результата
      val html = cartTpl(args)(ctx)
      Ok(html)
    }
  }


  protected object TxnResults {

    sealed trait TxnRes

    /** Ничего делать не требуется. */
    case object NothingToDo extends TxnRes

    /** Ордер был проведён.  */
    sealed case class OrderClosed(cart: IOrderWithItems) extends TxnRes

  }

  /**
    * Сабмит формы подтверждения корзины.
    *
    * @param onNodeId На каком узле сейчас находимся?
    * @return Редирект или страница оплаты.
    */
  def cartSubmit(onNodeId: String) = IsAdnNodeAdminPost(onNodeId, U.PersonNode, U.Contract).async { implicit request =>
    // Если цена нулевая, то контракт оформить как выполненный. Иначе -- заняться оплатой.
    // Чтение ордера, item'ов, кошелька, etc и их возможную модификацию надо проводить внутри одной транзакции.
    for {
      personNode  <- request.user.personNodeFut
      enc         <- bill2Util.ensureNodeContract(personNode, request.user.mContractOptFut)

      // Дальше надо бы делать транзакцию
      res <- {
        lazy val logPrefix = s"cartSubmit($onNodeId):"
        val contractId = enc.mc.id.get

        // Собрать логику SQL-запросов.
        val dbAction = for {
          // Прочитать текущую корзину
          cart0 <- bill2Util.prepareCartTxn( contractId )

          // На основе наполнения корзины нужно выбрать дальнейший путь развития событий:
          (txnRes: TxnResults.TxnRes) <- {
            LOGGER.trace(s"$logPrefix Contract[$contractId]: User's[${request.user.personIdOpt.orNull}] cart order[${cart0.morder.id.orNull}]. ${cart0.mitems.size} items in cart.")
            if (cart0.mitems.isEmpty) {
              // Корзина пуста. Should not happen.
              LOGGER.trace(logPrefix + " no items in the cart")
              DBIO.successful( TxnResults.NothingToDo )

            } else if ( !bill2Util.itemsHasPrice(cart0.mitems) ) {
              // Итемы есть, но всё бесплатно. Исполнить весь этот контракт прямо тут.
              LOGGER.trace(logPrefix + " Only FREE items in cart.")
              for (_ <- bill2Util.executeOrderAction(cart0)) yield {
                TxnResults.OrderClosed(cart0)
              }

            } else {
              // Товары есть, и они не бесплатны.
              LOGGER.trace(logPrefix + "There are unpaid items in cart.")

              // Считаем полную стоимость заказа-корзины.
              val totalPrices = bill2Util.items2pricesIter(cart0.mitems).toSeq
              // TODO Прочитать баланс, списать с баланса, проведя ордер. Или отправить в платежную систему для оплаты недостающего.
              // Узнаём текущее финансовое состояние юзера...
              ???
            }
          }
        } yield {
          // Сформировать результат работы экшена
          txnRes
        }

        // Отправить собранный DB-экшен на исполнение как транзакцию:
        import dbConfig.driver.api._
        dbConfig.db.run( dbAction.transactionally )
      }
    } yield {

      // Вернуть результат юзеру
      implicit val ctx = implicitly[Context]

      // TODO Породить события SioNotifier, если необходимо
      res match {
        // У юзера оказалась пустая корзина. Отредиректить в корзину с ошибкой.
        case TxnResults.NothingToDo =>
          Redirect( routes.LkBill2.cart(onNodeId) )
            .flashing( FLASH.ERROR -> ctx.messages("Cart.is.empty.No.items") )

        // Ордер был исполнен вместе с его наполнением.
        case TxnResults.OrderClosed(order2) =>
          // Уведомить об ордере.
          sn.publish( OrderStatusChanged(order2.morder) )
          // Уведомить об item'ах.
          for (mitem <- order2.mitems) {
            sn.publish( ItemStatusChanged(mitem) )
          }
          // Отправить юзера на страницу "Спасибо за покупку"
          Redirect( routes.LkBill2.thanksForBuy(onNodeId) )
      }
    }
  }

}
