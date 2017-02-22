package controllers.cbill

import controllers.{SioController, routes}
import io.suggest.bill.{MGetPriceResp, MPrice}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{IMItems, ItemStatusChanged, MItem}
import io.suggest.mbill2.m.order.{IMOrders, MOrder, OrderStatusChanged}
import io.suggest.model.common.OptId
import io.suggest.model.n2.node.MNode
import io.suggest.util.logs.IMacroLogs
import models.blk.{IRenderArgs, RenderArgs}
import models.mbill._
import models.mctx.Context
import models.req.INodeReq
import play.api.i18n.Messages
import util.acl._
import util.blocks.{BgImg, BlocksConf, IBlkImgMakerDI}
import util.billing.IBill2UtilDi
import util.di.ILogoUtilDi
import views.html.lk.billing.order._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.17 14:03
  * Description: Поддержка работы с заказами (кроме корзины).
  */
trait LkBillOrders
  extends SioController
  with IBill2UtilDi
  with IMacroLogs
  with IIsAdnNodeAdmin
  with IMOrders
  with IMItems
  with ICanViewOrder
  with ICanAccessItemDi
  with ILogoUtilDi
  with IBlkImgMakerDI
{

  import mCommonDi._
  import canAccessItem.CanAccessItemPost


  /** Сколько ордеров рисовать на одной странице списка ордеров? */
  private def ORDERS_PER_PAGE = 10

  /** Отображение карточек в таком вот размере. */
  private def ADS_SZ_MULT = 0.33F


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

    // Получить item'ы для рендера содержимого текущего заказа.
    val mitemsFut = bill2Util.orderItems(orderId)
    val ctxFut = request.user.lkCtxDataFut.map { implicit lkCtxData =>
      implicitly[Context]
    }

    // Собрать данные для рендера списка узлов.
    val mItemsTplArgsFut = _mItemsTplArgs(mitemsFut, ctxFut)

    // Собрать карту балансов по id. Она нужна для рендера валюты транзакции. Возможно ещё для чего-либо пригодится.
    // Нет смысла цеплять это об необязательно найденную транзакцию, т.к. U.Lk наверху гарантирует, что mBalancesFut уже запущен на исполнение.
    val mBalsMapFut = for {
      mBals <- request.user.mBalancesFut
    } yield {
      OptId.els2idMap[Gid_t, MBalance](mBals)
    }

    // Посчитать текущую стоимость заказа:
    val orderPricesFut = slick.db.run {
      bill2Util.getOrderPrices(orderId)
    }

    // Отрендерить ответ, когда всё будет готово.
    for {
      txns          <- txnsFut
      orderPrices   <- orderPricesFut
      mBalsMap      <- mBalsMapFut
      ctx           <- ctxFut
      mItemsTplArgs <- mItemsTplArgsFut
    } yield {

      val tplArgs = MShowOrderTplArgs(
        _underlying   = mItemsTplArgs,
        morder        = request.morder,
        orderPrices   = orderPrices,
        txns          = txns,
        balances      = mBalsMap
      )
      Ok( ShowOrderTpl(tplArgs)(ctx) )
    }
  }


  /** Показать ордеры, относящиеся к текущему юзеру.
    * Была идея, чтобы показывать только ордеры, относящиеся к текущему узлу, но это наверное слишком сложно
    * и неочевидно для юзеров.
    *
    * @param onNodeId id узла.
    * @param page Номер страницы.
    * @return Страница со таблицей-списком заказов. Свежие заказы сверху.
    */
  def orders(onNodeId: MEsUuId, page: Int) = isAdnNodeAdmin.Get(onNodeId, U.Lk).async { implicit request =>
    lazy val logPrefix = s"nodeOrders($onNodeId, $page):"

    // Слишком далёкую страницу - отсеивать.
    if (page > 150)
      throw new IllegalArgumentException(s"$logPrefix page number too high")

    // Начинаем плясать от контракта...
    val contractIdOptFut = request.user.contractIdOptFut

    // Получить интересующие ордеры из базы.
    val ordersFut = contractIdOptFut.flatMap { contractIdOpt =>
      // Если нет контракта, то искать ничего не надо.
      contractIdOpt.fold [Future[Seq[MOrder]]] {
        LOGGER.trace(s"$logPrefix No contract - no orders for user ${request.user.personIdOpt.orNull}")
        Future.successful( Nil )

      } { contractId =>
        val perPage = ORDERS_PER_PAGE
        slick.db.run {
          bill2Util.findLastOrders(
            contractId  = contractId,
            limit       = perPage,
            offset      = page * perPage
          )
        }
      }
    }

    // Посчитать общее кол-во заказов у юзера.
    val ordersTotalFut = contractIdOptFut.flatMap { contractIdOpt =>
      contractIdOpt.fold( Future.successful(0) ) { contractId =>
        slick.db.run {
          mOrders.countByContractId(contractId)
        }
      }
    }

    // На след.шагах нужно множество id'шников ордеров...
    val orderIdsFut = for (orders <- ordersFut) yield {
      val r = OptId.els2idsSet(orders)
      LOGGER.trace(s"$logPrefix Found ${orders.size} orders: ${r.mkString(", ")}")
      r
    }

    // Надо рассчитать стоимости ордеров. Для ускорения, сделать это пакетно c GROUP BY.
    val orderPricesFut = orderIdsFut.flatMap { orderIds =>
      slick.db.run {
        bill2Util.getOrdersPrices(orderIds)
      }
    }

    // Узнать id ордера-корзины.
    val cartOrderIdOptFut = contractIdOptFut.flatMap { contractIdOpt =>
      FutureUtil.optFut2futOpt(contractIdOpt) { contractId =>
        slick.db.run {
          bill2Util.getCartOrderId(contractId)
        }
      }
    }

    // По идее, получение lkCtxData уже запущено, но лучше убедится в этом.
    val lkCtxDataFut = request.user.lkCtxDataFut

    // Отрендерить ответ, когда всё будет готово.
    for {
      orders          <- ordersFut
      prices          <- orderPricesFut
      cartOrderIdOpt  <- cartOrderIdOptFut
      ordersTotal     <- ordersTotalFut
      lkCtxData       <- lkCtxDataFut
    } yield {
      implicit val lkCtxData1 = lkCtxData

      val tplArgs = MOrdersTplArgs(
        mnode         = request.mnode,
        orders        = orders,
        prices        = prices,
        cartOrderId   = cartOrderIdOpt,
        ordersTotal   = ordersTotal,
        page          = page,
        ordersPerPage = ORDERS_PER_PAGE
      )
      Ok( OrdersTpl(tplArgs) )
    }
  }




  private def _mItemsTplArgs(mitemsFut: Future[Seq[MItem]], ctxFut: Future[Context])(implicit request: INodeReq[_]): Future[MItemsTplArgs] = {
    // Собрать id узлов, на которые завязаны item'ы.
    val itemNodeIdsFut = for (mitems <- mitemsFut) yield {
      mitems.iterator
        .map(_.nodeId)
        .toSet
    }
    // Собрать id узлов-ресиверов.
    val rcvrIdsFut = for (mitems <- mitemsFut) yield {
      mitems.iterator
        .filter( _.iType != MItemTypes.GeoTag )
        .flatMap( _.rcvrIdOpt )
        .toSet
    }

    // Узнать все узлы, но в виде единой карты.
    val allNodesMapFut = for {
      itemNodeIds <- itemNodeIdsFut
      rcvrIds     <- rcvrIdsFut
      allNodeIds  = itemNodeIds ++ rcvrIds
      allNodes    <- mNodesCache.multiGet(allNodeIds)
    } yield {
      OptId.els2idMap[String, MNode](allNodes)
    }

    lazy val logPrefix = s"cart[${System.currentTimeMillis()}]:"

    // Собрать последовательность карточек для рендера, которые относятся к найденным mitem'ам:
    val itemNodesFut = for {
      mitems      <- mitemsFut
      allNodeMap  <- allNodesMapFut
    } yield {
      mitems
        .map(_.nodeId)
        .distinct
        .flatMap { itemNodeId =>
          val r = allNodeMap.get(itemNodeId)
          if (r.isEmpty)
            LOGGER.warn(s"$logPrefix Cannot find item node $itemNodeId")
          r
        }
    }

    // Получаем мультипликатор размера отображения.
    val szMult = ADS_SZ_MULT

    // Собрать карту аргументов для рендера карточек
    val node2brArgsMapFut: Future[Map[String, IRenderArgs]] = for {
      ctx         <- ctxFut
      itemNodeIds <- itemNodeIdsFut
      allNodesMap <- allNodesMapFut
      brArgss     <- Future.sequence {
        // Интересуют только ноды, которые можно рендерить как рекламные карточки.
        val devScrOpt = ctx.deviceScreenOpt
        for {
          (nodeId, mad) <- allNodesMap
          if itemNodeIds.contains(nodeId) && mad.ad.nonEmpty
        } yield {
          for {
            bgOpt <- BgImg.maybeMakeBgImgWith(mad, blkImgMaker, szMult, devScrOpt)
          } yield {
            val ra = RenderArgs(
              mad       = mad,
              bc        = BlocksConf.applyOrDefault(mad),
              withEdit  = false,
              bgImg     = bgOpt,
              szMult    = szMult,
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
    val node2itemsMapFut = for {
      mitems <- mitemsFut
    } yield {
      mitems.groupBy(_.nodeId)
    }

    // Собрать карту картинок-логотипов узлов. ADN-узлы нельзя ренедрить как карточки, но можно взять логотипы.
    val node2logoMapFut = for {
      ctx         <- ctxFut
      itemNodeIds <- itemNodeIdsFut
      allNodesMap <- allNodesMapFut
      devScreenOpt = ctx.deviceScreenOpt
      node2logos  <- Future.sequence {
        for {
          (nodeId, mnode) <- allNodesMap
          // Пока интересуют только логотипы узлов, которые вместо рекламных карточек будут отображаться.
          if itemNodeIds.contains(nodeId) && mnode.ad.isEmpty
        } yield {
          // Готовим логотип данного узла... Если логотипа нет, то тут будет синхронное None.
          for {
            logoOptRaw     <- logoUtil.getLogoOfNode(mnode)
            logoOptScr     <- logoUtil.getLogoOpt4scr(logoOptRaw, devScreenOpt)
          } yield {
            for (logo <- logoOptScr; nodeId <- mnode.id) yield {
              nodeId -> logo
            }
          }
        }
      }
    } yield {
      node2logos
        .iterator
        .flatten
        .toMap
    }

    // Рендер и возврат ответа
    for {
      allNodesMap     <- allNodesMapFut
      nodes           <- itemNodesFut
      node2logosMap   <- node2logoMapFut
      node2brArgsMap  <- node2brArgsMapFut
      node2itemsMap   <- node2itemsMapFut
    } yield {
      MItemsTplArgs(
        mnode         = request.mnode,
        nodesMap      = allNodesMap,
        itemNodes     = nodes,
        node2logo     = node2logosMap,
        node2brArgs   = node2brArgsMap,
        node2items    = node2itemsMap
      )
    }
  }


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

    // Параллельно собираем контекст рендера
    val ctxFut = for {
      lkCtxData <- request.user.lkCtxDataFut
    } yield {
      implicit val ctxData = lkCtxData
      implicitly[Context]
    }

    // Начать сборку аргументов для рендера списка item'ов.
    val mItemsTplArgsFut = _mItemsTplArgs(mitemsFut, ctxFut)

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
      ctx             <- ctxFut
      mItemsTplArgs   <- mItemsTplArgsFut
      totalPricing    <- totalPricingFut
    } yield {
      // Сборка аргументов для вызова шаблона
      val args = MCartTplArgs(
        _underlying   = mItemsTplArgs,
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
    lazy val logPrefix = s"cartClear(u=${request.user.personIdOpt.orNull},on=$onNodeId):"

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
          case _: NoSuchElementException =>
            LOGGER.trace(s"$logPrefix Unable to clear cart, because contract or cart order does NOT exists")
          case _ =>
            LOGGER.warn(s"$logPrefix Cart clear failed", ex)
        }
        0
      }
      // Независимо от исхода, вернуть редирект куда надо.
      .map { itemsDeleted =>
        LOGGER.trace(s"$logPrefix $itemsDeleted items deleted.")
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

    for (rowsDeleted <- delFut) yield {
      val resp0 = Redirect(r)
      if (rowsDeleted == 1) {
        resp0
      } else {
        LOGGER.warn(s"$logPrefix MItems.deleteById() returned invalid deleted rows count: $rowsDeleted")
        resp0.flashing(FLASH.ERROR -> implicitly[Messages].apply("Something.gone.wrong"))
      }
    }
  }


}
