package controllers.cbill

import controllers.{SioController, routes}
import io.suggest.bill.{MGetPriceResp, MPrice}
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{IMItems, ItemStatusChanged, MItem}
import io.suggest.mbill2.m.order.OrderStatusChanged
import io.suggest.util.logs.IMacroLogs
import models.blk.{IRenderArgs, RenderArgs}
import models.mbill.MCartIdeas
import models.mctx.Context
import models.mlk.bill.{MCartItem, MCartTplArgs}
import util.acl.{ICanAccessItemDi, IIsAdnNodeAdmin}
import util.billing.IBill2UtilDi
import util.blocks.{BgImg, BlocksConf, IBlkImgMakerDI}
import views.html.lk.billing.order._

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
  with IMacroLogs
  with IIsAdnNodeAdmin
  with IMItems
  with ICanAccessItemDi
  with IBlkImgMakerDI
{

  import mCommonDi._
  import canAccessItem.CanAccessItemPost

  /** Отображение карточек в таком вот размере. */
  private def ADS_SZ_MULT = 0.25F

  /**
    * Рендер страницы с корзиной покупок юзера в рамках личного кабинета узла.
    *
    * @param onNodeId На каком узле сейчас смотрим корзину текущего юзера?
    * @param r Куда производить возврат из корзины.
    * @return 200 ОК с html страницей корзины.
    */
  def cart(onNodeId: String, r: Option[String]) = isAdnNodeAdmin.Get(onNodeId, U.Lk, U.ContractId).async { implicit request =>

    // Узнать id контракта юзера. Сам контракт не важен.
    val mcIdOptFut = request.user.contractIdOptFut

    // Найти ордер-корзину юзера в базе биллинга:
    val cartOptFut = mcIdOptFut.flatMap { mcIdOpt =>
      FutureUtil.optFut2futOpt(mcIdOpt) { mcId =>
        slick.db.run {
          bill2Util.getCartOrder(mcId)
        }
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
      val wantAdIds = mitems.iterator.map(_.nodeId).toSet
      mNodesCache.multiGet(wantAdIds)
    }

    // Параллельно собираем контекст рендера
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
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
            bgOpt <- BgImg.maybeMakeBgImgWith(mad, blkImgMaker, szMult, ctx.deviceScreenOpt)
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
      mitems.groupBy(_.nodeId)
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
      MGetPriceResp(
        prices = MPrice.toSumPricesByCurrency(mitems).values
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
      val html = CartTpl(args)(ctx)
      Ok(html)
    }
  }



  /**
    * Сабмит формы подтверждения корзины.
    *
    * @param onNodeId На каком узле сейчас находимся?
    * @return Редирект или страница оплаты.
    */
  def cartSubmit(onNodeId: String) = isAdnNodeAdmin.Post(onNodeId, U.PersonNode, U.Contract).async { implicit request =>
    // Если цена нулевая, то контракт оформить как выполненный. Иначе -- заняться оплатой.
    // Чтение ордера, item'ов, кошелька, etc и их возможную модификацию надо проводить внутри одной транзакции.
    for {
      personNode  <- request.user.personNodeFut
      enc         <- bill2Util.ensureNodeContract(personNode, request.user.mContractOptFut)
      contractId  = enc.mc.id.get

      // Дальше надо бы делать транзакцию
      res <- {
        // Произвести чтение, анализ и обработку товарной корзины:
        slick.db.run {
          import slick.profile.api._
          val dbAction = for {
          // Прочитать текущую корзину
            cart0   <- bill2Util.prepareCartTxn( contractId )
            // На основе наполнения корзины нужно выбрать дальнейший путь развития событий:
            txnRes  <- bill2Util.maybeExecuteOrder(cart0)
          } yield {
            // Сформировать результат работы экшена
            txnRes
          }
          // Форсировать весь этот экшен в транзакции:
          dbAction.transactionally
        }
      }

    } yield {

      // Начать сборку http-ответа для юзера
      implicit val ctx = implicitly[Context]

      res match {

        // Недостаточно бабла на балансах юзера в sio, это нормально. TODO Отправить в платежную систему...
        case r: MCartIdeas.NeedMoney =>
          Redirect( controllers.pay.routes.PayYaka.payForm(r.cart.morder.id.get, onNodeId) )

        // Хватило денег на балансах или они не потребовались. Такое бывает в т.ч. после возврата юзера из платежной системы.
        // Ордер был исполнен вместе с его наполнением.
        case oc: MCartIdeas.OrderClosed =>
          // Уведомить об ордере.
          sn.publish( OrderStatusChanged(oc.cart.morder) )
          // Уведомить об item'ах.
          for (mitem <- oc.cart.mitems) {
            sn.publish( ItemStatusChanged(mitem) )
          }
          // Отправить юзера на страницу "Спасибо за покупку"
          Redirect( routes.LkBill2.thanksForBuy(onNodeId) )

        // У юзера оказалась пустая корзина. Отредиректить в корзину с ошибкой.
        case MCartIdeas.NothingToDo =>
          Redirect( routes.LkBill2.cart(onNodeId) )
            .flashing( FLASH.ERROR -> ctx.messages("Your.cart.is.empty") )
      }
    }
  }


  /**
    * Очистить корзину покупателя.
    *
    * @param onNodeId На какой ноде в ЛК происходит действие очистки.
    * @param r Адрес страницы для возвращения юзера.
    *          Если пусто, то юзер будет отправлен на страницу своей пустой корзины.
    * @return Редирект.
    */
  def cartClear(onNodeId: String, r: Option[String]) = isAdnNodeAdmin.Post(onNodeId, U.ContractId).async { implicit request =>
    lazy val logPrefix = s"cartClear($onNodeId):"

    request.user
      .contractIdOptFut
      // Выполнить необходимые операции в БД биллинга.
      .flatMap { contractIdOpt =>
        // Если корзина не существует, то делать ничего не надо.
        val contractId = contractIdOpt.get

        // Запускаем без transaction на случай невозможности удаления ордера корзины.
        slick.db.run {
          bill2Util.clearCart(contractId)
        }
      }
      // Подавить и залоггировать возможные ошибки.
      .recover { case ex: Exception =>
        ex match {
          case ex1: NoSuchElementException =>
            LOGGER.trace(s"$logPrefix Unable to clear cart, because contract or cart order does NOT exists")
          case _ =>
            LOGGER.warn(s"$logPrefix Cart clear failed", ex)
        }
        0
      }
      // Независимо от исхода, вернуть редирект куда надо.
      .map { itemsDeleted =>
        RdrBackOr(r)(routes.LkBill2.cart(onNodeId))
      }
  }


  /**
    * Удалить item из корзины, вернувшись затем на указанную страницу.
    *
    * @param itemId id удаляемого item'а.
    * @param r Обязательный адрес для возврата по итогам действа.
    * @return Редирект в r.
    */
  def cartDeleteItem(itemId: Gid_t, r: String) = CanAccessItemPost(itemId, edit = true).async { implicit request =>
    // Права уже проверены, item уже получен. Нужно просто удалить его.
    val delFut0 = slick.db.run {
      mItems.deleteById(itemId)
    }

    lazy val logPrefix = s"cartDeleteItem($itemId):"

    // Подавить возможные ошибки удаления.
    val delFut = delFut0.recover { case ex: Throwable =>
      LOGGER.error(s"$logPrefix Item delete failed", ex)
      0
    }

    implicit val ctx = implicitly[Context]
    for (rowsDeleted <- delFut) yield {
      val resp0 = Redirect(r)
      if (rowsDeleted == 1) {
        resp0
      } else {
        LOGGER.warn(s"$logPrefix MItems.deleteById() returned invalid deleted rows count: $rowsDeleted")
        resp0.flashing(FLASH.ERROR -> ctx.messages("Something.gone.wrong"))
      }
    }
  }

}
