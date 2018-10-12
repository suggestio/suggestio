package controllers.cbill

import akka.stream.scaladsl.{Keep, Sink, Source}
import controllers.{SioController, routes}
import io.suggest.bill.cart.{MCartConf, MCartInit, MOrderContent}
import io.suggest.bill.{MCurrency, MPrice}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.MEsUuId
import io.suggest.i18n.MsgCodes
import io.suggest.init.routed.MJsInitTargets
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{IMItems, MItem}
import io.suggest.mbill2.m.order.{IMOrders, MOrder, MOrderStatuses}
import io.suggest.mbill2.m.txn.{MTxn, MTxnPriced}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.model.play.qsb.QsbSeq
import io.suggest.primo.id.OptId
import io.suggest.req.ReqUtil
import io.suggest.util.logs.IMacroLogs
import models.mbill._
import models.mctx.Context
import models.req._
import util.acl._
import util.billing.IBill2UtilDi
import views.html.lk.billing.order._
import japgolly.univeq._
import play.api.libs.json.Json
import play.api.mvc.{ActionBuilder, AnyContent}
import util.TplDataFormatUtil
import util.ad.IJdAdUtilDi
import util.adv.geo.IAdvGeoRcvrsUtilDi

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
  with IIsNodeAdmin
  with IMOrders
  with IMItems
  with ICanViewOrder
  with ICanAccessItemDi
  with IIsAuth
  with IJdAdUtilDi
  with IAdvGeoRcvrsUtilDi
{

  protected val reqUtil: ReqUtil

  import mCommonDi._


  /** Сколько ордеров рисовать на одной странице списка ордеров? */
  private def ORDERS_PER_PAGE = 10


  /* Код для вычисления переплат по транзакциям был в showOrder(), удалён после 09a112d9b548e68be06b7b5b057ead86dccc677e */


  /** Показать ордеры, относящиеся к текущему юзеру.
    * Была идея, чтобы показывать только ордеры, относящиеся к текущему узлу, но это наверное слишком сложно
    * и неочевидно для юзеров.
    *
    * @param onNodeId id узла.
    * @param page Номер страницы.
    * @return Страница со таблицей-списком заказов. Свежие заказы сверху.
    */
  def orders(onNodeId: MEsUuId, page: Int) = csrf.AddToken {
    isNodeAdmin(onNodeId, U.Lk).async { implicit request =>
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
  }


  /** Страница для react-корзины.
    *
    * @param onNodeId На каком узле рендерится страница.
    * @param orderIdOpt Точный id заказа. Если None, то только корзина.
    * @param r Адресок для возврата.
    * @return 200 OK + HTML-страница с данными для инициализации react-формы корзины.
    */
  def orderPage(onNodeId: String, orderIdOpt: Option[Gid_t], r: Option[String]) = csrf.AddToken {
    // Инициализация, необходимая внутри тела экшена.
    val uinits = U.Lk :: U.ContractId :: Nil

    // Экшен-билдер. В зависимости от наличия или отсутствия orderId, собрать нужный билдер:
    val ab = orderIdOpt.fold [ActionBuilder[MNodeOrderOptReq, AnyContent]] {
      isNodeAdmin(onNodeId, uinits: _*).andThen(
        new reqUtil.ActionTransformerImpl[MNodeReq, MNodeOrderOptReq] {
          override protected def transform[A](request: MNodeReq[A]): Future[MNodeOrderOptReq[A]] = {
            val req2 = MNodeOrderOptReq( None, request.mnode, request.user, request )
            Future.successful( req2 )
          }
        }
      )
    } { orderId =>
      canViewOrder(orderId, Some(onNodeId), uinits: _*).andThen(
        new reqUtil.ActionTransformerImpl[MNodeOptOrderReq, MNodeOrderOptReq] {
          override protected def transform[A](request: MNodeOptOrderReq[A]): Future[MNodeOrderOptReq[A]] = {
            val req2 = MNodeOrderOptReq( Some(request.morder), request.mnodeOpt.get, request.user, request )
            Future.successful( req2 )
          }
        }
      )
    }

    // Сборка тела экшена, который вернёт страницу под react-форму.
    ab.async { implicit request =>
      // Найти ордер-корзину юзера в базе биллинга по id контракта:
      val requestOrderIdOpt = request.morderOpt.flatMap(_.id)
      val orderIdOptFut = requestOrderIdOpt.fold {
        request.user.contractIdOptFut.flatMap { mcIdOpt =>
          FutureUtil.optFut2futOpt(mcIdOpt) { mcId =>
            slick.db.run {
              bill2Util.getCartOrderId(mcId)
            }
          }
        }
      } { _ => Future.successful(requestOrderIdOpt) }

      // Готовить контекст для рендера:
      val ctxFut = for {
        lkCtxData <- request.user.lkCtxDataFut
      } yield {
        implicit val ctxData = lkCtxData.withJsInitTargets(
          MJsInitTargets.LkCartPageForm :: lkCtxData.jsInitTargets
        )
        implicitly[Context]
      }

      // Отрендерить данные формы в JSON.
      val formInitB64Fut = for {
        orderIdOpt  <- orderIdOptFut
      } yield {
        val minit = MCartInit(
          conf = MCartConf(
            orderId  = orderIdOpt,
            onNodeId = request.mnode.id,
          )
        )
        Json
          .toJson( minit )
          .toString()
      }

      // Данные формы для инициализации.
      for {
        orderIdOpt      <- orderIdOptFut
        formInitB64     <- formInitB64Fut
        ctx             <- ctxFut
      } yield {
        val html = OrderPageTpl(
          orderIdOpt    = orderIdOpt,
          mnode         = request.mnode,
          formStateB64  = formInitB64
        )(ctx)
        Ok( html )
      }
    }
  }


  /** Запрос за данными корзины (JSON для react-формы).
    *
    * @param orderId id заказа, если известно.
    *                None - значит корзина.
    * @return 200 OK + JSON с содержимым запрошенного ордера.
    */
  def getOrder(orderId: Option[Gid_t]) = csrf.Check {
    // Два варианта action-обёртки ACL, в зависимости от наличия/отсутствия orderId:
    val ab = orderId.fold[ActionBuilder[MOrderOptReq, AnyContent]] {
      // Просмотр только текущей корзины. Юзер всегда может глядеть в свою корзину.
      isAuth().andThen(
        new reqUtil.ActionTransformerImpl[MReq, MOrderOptReq] {
          override protected def transform[A](request: MReq[A]): Future[MOrderOptReq[A]] = {
            val req2 = MOrderOptReq( None, request )
            Future.successful( req2 )
          }
        }
      )
    } { orderId1 =>
      // Просмотр одного конкретного ордера. Возможно, корзины.
      canViewOrder( orderId1, onNodeId = None, U.ContractId ).andThen(
        new reqUtil.ActionTransformerImpl[MNodeOptOrderReq, MOrderOptReq] {
          override protected def transform[A](request: MNodeOptOrderReq[A]): Future[MOrderOptReq[A]] = {
            val req2 = MOrderOptReq( Some(request.morder), request )
            Future.successful( req2 )
          }
        }
      )
    }

    // Наконец, сборка данных для JSON-ответа:
    ab.async { implicit request =>
      for {
        orderContents <- _getOrderContents( request.morderOpt )
      } yield {
        Ok( Json.toJson(orderContents) )
      }
    }
  }


  /** Общий код сборки инстанса MOrderContents. */
  private def _getOrderContents(morderOpt: Option[MOrder])(implicit request: IReq[_]): Future[MOrderContent] = {
    lazy val logPrefix = s"_getOrderContents(${morderOpt.flatMap(_.id).fold("cart")(_.toString)}):"

    // Получить текущий ордер-корзину, если в реквесте нет:
    val morderOptFut = FutureUtil.opt2futureOpt( morderOpt ) {
      request.user
        .contractIdOptFut
        .flatMap { contractIdOpt =>
          LOGGER.trace(s"$logPrefix contractId=${contractIdOpt.orNull} personId#${request.user.personIdOpt.orNull}")
          FutureUtil.optFut2futOpt( contractIdOpt ) { contractId =>
            slick.db.run {
              bill2Util.getCartOrder( contractId )
            }
          }
        }
    }

    // Получить item'ы для текущего ордера:
    val mitemsFut = morderOptFut.flatMap { morderOpt =>
      morderOpt
        .flatMap(_.id)
        .fold [Future[Seq[MItem]]] ( Future.successful(Nil) ) { orderId =>
          slick.db.run {
            mItems.findByOrderId( orderId )
          }
        }
    }

    // Собрать транзакции, если это НЕ ордер-корзина:
    val mTxnsCurFut = morderOpt
      .filter(_.status !=* MOrderStatuses.Draft)
      .flatMap(_.id)
      .fold [Future[Seq[(MTxn, MCurrency)]]] ( Future.successful(Nil) ) { orderId =>
        slick.db.run {
          bill2Util.getOrderTxnsWithCurrencies( orderId )
        }
      }

    val ctx = implicitly[Context]

    // Надо добавить ценники к найденным транзакциям.
    val mTxnsPricedFut = for {
      txnsCur <- mTxnsCurFut
    } yield {
      for {
        (mtxn, mcurrency) <- txnsCur
      } yield {
        val price0 = MPrice(mtxn.amount, mcurrency)
        MTxnPriced(
          txn   = mtxn,
          price = TplDataFormatUtil.setFormatPrice( price0 )(ctx)
        )
      }
    }

    // Рассчёт полной стоимости заказа.
    val orderPricesFut = morderOptFut.flatMap { morderOpt =>
      morderOpt
        .flatMap(_.id)
        .fold [Future[Seq[MPrice]]] ( Future.successful(Nil) ) { orderId =>
          slick.db.run {
            bill2Util.getOrderPrices( orderId )
          }
          .map { prices =>
            for (mprice <- prices) yield
              TplDataFormatUtil.setFormatPrice(mprice)(ctx)
          }
        }
    }

    // Сборка всех искомых узлов (ресиверы, карточки) идёт одним multi-get:
    val allNodesMapFut = for {
      mitems <- mitemsFut
      nodeIds = mitems
        .iterator
        .flatMap { mitem =>
          mitem.nodeId ::
            mitem.rcvrIdOpt.toList
        }
        .toSet
      nodesMap <- mNodesCache.multiGetMap( nodeIds )
    } yield {
      nodesMap
    }

    // Собрать узлы-ресиверы, которые упоминаемые в items:
    val rcvrsFut = for {
      mitems <- mitemsFut
      rcvrsIds = mitems
        .iterator
        .flatMap(_.rcvrIdOpt)
        .toSet
      allNodesMap <- allNodesMapFut
    } yield {
      val iter = for {
        mnode  <- rcvrsIds.iterator.flatMap( allNodesMap.get )
        nodeId <- mnode.id
      } yield {
        MAdvGeoMapNodeProps(
          nodeId = nodeId,
          ntype   = mnode.common.ntype,
          // mnode.meta.colors,  // TODO Надо ли цвета рендерить?
          colors  = MColors.empty,
          hint    = mnode.guessDisplayNameOrId,
          // Пока без иконки. TODO Решить, надо ли иконку рендерить?
          icon    = None
        )
      }
      iter.toSeq
    }

    // Собрать множество nodeId из mitems:
    val itemNodeIdsFut = for (mitems <- mitemsFut) yield {
      mitems
        .iterator
        .map(_.nodeId)
        .toSet
    }

    // Отрендерить jd-карточки в JSON:
    val jdAdDatasFut = for {
      // Сбор id узлов, которые скорее всего являются карточками.
      itemNodeIds <- itemNodeIdsFut

      // Дождаться прочитанных узлов:
      allNodesMap <- allNodesMapFut
      adNodesMap = allNodesMap
        .filterKeys( itemNodeIds.contains )
        .filter { case (_, mnode) =>
          // Если не отфильтровать только карточки (могут быть и adn-узлы), то зафейлится jd-рендер.
          (mnode.common.ntype ==* MNodeTypes.Ad) &&
            mnode.extras.doc.nonEmpty
        }

      // Начинаем рендерить
      adDatas <- Future.traverse( adNodesMap.values ) { mad =>
        // Для ускорения рендера - каждую карточку отправляем сразу в фон:
        Future {
          val mainTpl = jdAdUtil.getMainBlockTpl( mad )
          // Убрать wide-флаг в main strip'е, иначе будет плитка со строкой-дыркой.
          val mainNonWideTpl = jdAdUtil.setBlkWide(mainTpl, wide2 = false)
          val edges2 = jdAdUtil.filterEdgesForTpl(mainNonWideTpl, mad.edges)

          jdAdUtil
            .mkJdAdDataFor
            .show(
              nodeId        = mad.id,
              nodeEdges     = edges2,
              tpl           = mainNonWideTpl,
              // Тут по идее надо четверть или половину, но с учётом плотности пикселей можно округлить до 1.0. Это и нагрузку снизит.
              szMult        = 1.0f,
              allowWide     = false,
              forceAbsUrls  = false
            )(ctx)
            .execute()
        }
          .flatten
      }

    } yield {
      adDatas
    }

    // Надо отрендерить ценники на сервере:
    val mitems2Fut = for {
      mitems <- mitemsFut
    } yield {
      for (mitem <- mitems) yield {
        mitem.withPrice(
          TplDataFormatUtil.setFormatPrice(mitem.price)(ctx)
        )
      }
    }

    // Отбираем adn-узлы, которые заявлены в item.nodeId, рендерим в props+logo, объединяем с отрендеренными ресиверами
    val itemAdnNodesFut = for {

      itemNodeIds <- itemNodeIdsFut
      allNodesMap <- allNodesMapFut

      // Оставляем только adn-узлы.
      adnNodes = allNodesMap
        .filterKeys( itemNodeIds.contains )
        // Лениво фильтруем в immutable-коллекцию:
        .valuesIterator
        // Здесь интересны только adn-узлы:
        .filter { mnode =>
          mnode.common.ntype ==* MNodeTypes.AdnNode
        }
        .toStream

      // Прогоняем через рендер базовых данных по узлам:
      adnNodesPropsShapes <- {
        advGeoRcvrsUtil.nodesAdvGeoPropsSrc(
          nodesSrc = Source( adnNodes ),
          // Т.к. интересует вертикальный/квадратный лого, то делаем приоритет на картинку приветствия.
          wcAsLogo = true
        )
          .map(_._2)
          .toMat( Sink.seq )(Keep.right)
          .run()
      }

      // Объеденить с ресиверами из кода выше, т.к. все будут отправлены одной пачкой.
      rcvrs <- rcvrsFut

    } yield {
      adnNodesPropsShapes ++ rcvrs
    }

    // Наконец, сборка результата:
    for {
      morderOpt     <- morderOptFut
      mitems        <- mitems2Fut
      mTxnsPriced   <- mTxnsPricedFut
      itemAdnNodes  <- itemAdnNodesFut
      jdAdDatas     <- jdAdDatasFut
      orderPrices   <- orderPricesFut
    } yield {
      LOGGER.trace(s"$logPrefix order#${morderOpt.flatMap(_.id).orNull}, ${mitems.length} items, ${mTxnsPriced.length} txns, ${itemAdnNodes.length} adn-nodes, ${jdAdDatas.size} jd-ads")
      MOrderContent(
        order       = morderOpt,
        items       = mitems,
        txns        = mTxnsPriced,
        adnNodes    = itemAdnNodes,
        adsJdDatas  = jdAdDatas,
        orderPrices = orderPrices
      )
    }
  }



  /**
    * Сабмит формы подтверждения корзины.
    *
    * @param onNodeId На каком узле сейчас находимся?
    * @return Редирект или страница оплаты.
    */
  def cartSubmit(onNodeId: String) = csrf.Check {
    isNodeAdmin(onNodeId, U.PersonNode, U.Contract).async { implicit request =>
      // Если цена нулевая, то контракт оформить как выполненный. Иначе -- заняться оплатой.
      // Чтение ордера, item'ов, кошелька, etc и их возможную модификацию надо проводить внутри одной транзакции.
      for {
        personNode  <- request.user.personNodeFut
        enc         <- bill2Util.ensureNodeContract(personNode, request.user.mContractOptFut)
        contractId  = enc.mc.id.get

        // Дальше надо бы делать транзакцию
        // Произвести чтение, анализ и обработку товарной корзины:
        res <- slick.db.run {
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

      } yield {

        // Начать сборку http-ответа для юзера
        implicit val ctx = implicitly[Context]

        res match {

          // Недостаточно бабла на балансах юзера в sio, это нормально. TODO Отправить в платежную систему...
          case r: MCartIdeas.NeedMoney =>
            Redirect( controllers.pay.routes.PayYaka.payForm(r.cart.morder.id.get, onNodeId) )

          // Хватило денег на балансах или они не потребовались. Такое бывает в т.ч. после возврата юзера из платежной системы.
          // Ордер был исполнен вместе с его наполнением.
          case _: MCartIdeas.OrderClosed =>
            // Отправить юзера на страницу "Спасибо за покупку"
            Redirect( routes.LkBill2.thanksForBuy(onNodeId) )

          // У юзера оказалась пустая корзина. Отредиректить в корзину с ошибкой.
          case MCartIdeas.NothingToDo =>
            Redirect( routes.LkBill2.orderPage(onNodeId) )
              .flashing( FLASH.ERROR -> ctx.messages(MsgCodes.`Your.cart.is.empty`) )
        }
      }
    }
  }


  /** JSON API для удаления элементов корзины.
    *
    * @param itemIds
    * @return Обновлённая корзина в виде JSON-выхлопа MOrderContent.
    */
  def deleteItems(itemIds: QsbSeq[Gid_t]) = csrf.Check {
    canAccessItem(itemIds.items, edit = true).async { implicit request =>
      for {
        // Выполнить удаление item'ов
        deletedCount <- slick.db.run {
          mItems.deleteById( itemIds: _* )
        }

        // Получить обновлённые данные ордера-корзины:
        orderContents <- _getOrderContents( None )

      } yield {
        LOGGER.trace(s"cartDeleteItems(${itemIds.mkString(", ")}): Deleted $deletedCount items.")
        Ok( Json.toJson(orderContents) )
      }
    }
  }

}
