package controllers

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import io.suggest.adv.geo.AdvGeoConstants
import io.suggest.adv.info.{MNodeAdvInfo, MNodeAdvInfo4Ad}
import io.suggest.bill.cart.{MCartConf, MCartIdeas, MCartInit, MCartPayInfo, MCartSubmitQs, MCartSubmitResult, MOrderContent}
import io.suggest.bill.price.dsl.{MReasonType, MReasonTypes}
import io.suggest.bill.tf.daily.MTfDailyInfo
import io.suggest.bill.{MCurrency, MPrice}
import io.suggest.color.MColors
import io.suggest.common.fut.FutureUtil
import io.suggest.ctx.CtxData
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.jd.MJdConf
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.order.{MOrder, MOrderStatuses}
import io.suggest.mbill2.m.txn.{MTxn, MTxnPriced}
import io.suggest.media.{MMediaInfo, MMediaTypes}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.pay.{MPaySystem, MPaySystems}
import io.suggest.req.ReqUtil
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.xplay.qsb.QsbSeq
import japgolly.univeq._
import models.mbill._
import models.mctx.Context
import models.req._
import play.api.http.HttpVerbs
import play.api.libs.json.Json
import play.api.mvc.{ActionBuilder, AnyContent}
import util.TplDataFormatUtil
import util.acl._
import util.ad.JdAdUtil
import util.adn.NodesUtil
import util.adv.AdvUtil
import util.adv.geo.AdvGeoRcvrsUtil
import util.billing.{Bill2Util, TfDailyUtil}
import util.img.GalleryUtil
import util.mdr.MdrUtil
import util.pay.yookassa.YooKassaUtil
import util.sec.CspUtil
import views.html.lk.billing._
import views.html.lk.billing.order._

import javax.inject.Inject
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.16 19:17
  * Description: Контроллер биллинга второго поколения в личном кабинете.
  * Прошлый контроллер назывался MarketLkBilling.
  */
final class LkBill2 @Inject() (
                                sioControllerApi            : SioControllerApi,
                              )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import slickHolder.slick

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val tfDailyUtil = injector.instanceOf[TfDailyUtil]
  private lazy val galleryUtil = injector.instanceOf[GalleryUtil]
  private lazy val advUtil = injector.instanceOf[AdvUtil]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val nodesUtil = injector.instanceOf[NodesUtil]
  private lazy val canViewOrder = injector.instanceOf[CanViewOrder]
  private lazy val canAccessItem = injector.instanceOf[CanAccessItem]
  private lazy val canViewNodeAdvInfo = injector.instanceOf[CanViewNodeAdvInfo]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]
  private lazy val advGeoRcvrsUtil = injector.instanceOf[AdvGeoRcvrsUtil]
  private lazy val jdAdUtil = injector.instanceOf[JdAdUtil]
  private lazy val mdrUtil = injector.instanceOf[MdrUtil]
  private lazy val bill2Util = injector.instanceOf[Bill2Util]
  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val yooKassaUtil = injector.instanceOf[YooKassaUtil]
  private lazy val ignoreAuth = injector.instanceOf[IgnoreAuth]
  private lazy val canSubmitCart = injector.instanceOf[CanSubmitCart]
  implicit private lazy val mat = injector.instanceOf[Materializer]


  private def _dailyTfArgsFut(mnode: MNode, madOpt: Option[MNode] = None): Future[Option[MTfDailyTplArgs]] = {
    if (mnode.extras.adn.exists(_.isReceiver)) {
      import esModel.api._
      for {
        // Получить данные по тарифу.
        tfDaily   <- tfDailyUtil.nodeTf( mnode )
        // Прочитать календари, относящиеся к тарифу.
        calsRaw   <- mNodes.multiGet( tfDaily.calIds )
        calsMap = calsRaw
          .iterator
          .filter { mnode =>
            mnode.common.ntype ==* MNodeTypes.Calendar
          }
          .map { mnode =>
            mnode.id.get -> mnode
          }
          .toMap
      } yield {

        // Подготовить инфу по ценам на карточку, если она задана.
        val madTfOpt = for {
          mad <- madOpt
          blockModulesCount <- advUtil.adModulesCount( mad )
        } yield {
          val madTf = tfDaily.withClauses(
            tfDaily.clauses
              .view
              .mapValues { mdc =>
                mdc.withAmount(
                  mdc.amount * blockModulesCount
                )
              }
              .toMap
          )
          MAdTfInfo(blockModulesCount, madTf)
        }

        val args1 = MTfDailyTplArgs(
          mnode     = mnode,
          tfDaily   = tfDaily,
          madTfOpt  = madTfOpt,
          calsMap   = calsMap,
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
  def nodeAdvInfo(nodeId: String, forAdId: Option[String]) = csrf.Check {
    canViewNodeAdvInfo(nodeId, forAdId).async { implicit request =>
      val galleryImgsFut = galleryUtil.galleryImgs( request.mnode )
      implicit val ctx = implicitly[Context]

      // Собрать картинки
      val galleryFut = for {
        galleryImgs <- galleryImgsFut
        mediaHostsMapFut = nodesUtil.nodeMediaHostsMap(
          gallery = galleryImgs
        )
        galleryCalls <- galleryUtil.renderGalleryCdn( galleryImgs, mediaHostsMapFut )(ctx)
      } yield {
        for {
          (mimg, galleryCall) <- galleryCalls
        } yield {
          MMediaInfo(
            giType  = MMediaTypes.Image,
            url     = galleryCall.url,
            contentType = mimg.dynImgId.imgFormat.get.mime,
            // thumb'ы: Не отображаются на экране из-за особенностей вёрстки; в дизайне не предусмотрены.
            thumb   = None, /*Some(
            MMediaInfo(
              giType = MMediaTypes.Image,
              url    = dynImgUtil.thumb256Call(mimg, fillArea = true).url
            )
          )*/
          )
        }
      }

      // Собрать итоговый ответ клиенту:
      for {
        // Собрать данные по тарифу.
        tfInfo      <- tfDailyUtil.getTfInfo( request.mnode )(ctx)
        // Подготовить данные по тарифу в контексте текущей карточки:
        tfDaily4AdOpt = {
          for {
            adProdReq <- request.adProdReqOpt
          } yield {
            val tf_clauses_LENS = MTfDailyInfo.clauses
            def __tfClausesMultBy(multBy: Double) = {
              tf_clauses_LENS.modify {
                _ .view
                  .mapValues { p0 =>
                    val p2 = p0 * multBy
                    TplDataFormatUtil.setFormatPrice( p2 )(ctx)
                  }
                  .toMap
              }
            }

            val blocksCount = advUtil
              .adModulesCount( adProdReq.mad )
              .get

            // On tag prices:
            val tdDailyTag = __tfClausesMultBy( blocksCount )(tfInfo)

            // On-main-screen prices:
            val tfDaily4AdOms = __tfClausesMultBy( AdvGeoConstants.ON_MAIN_SCREEN_MULT )(tdDailyTag)

            MNodeAdvInfo4Ad(
              blockModulesCount = blocksCount,
              tfDaily           = {
                Map.empty[MReasonType, MTfDailyInfo] +
                (MReasonTypes.Tag -> tdDailyTag) +
                (MReasonTypes.OnMainScreen -> tfDaily4AdOms)
              },
            )
          }
        }
        gallery     <- galleryFut
      } yield {
        // Собрать финальный инстанс модели аргументов для рендера:
        val m = MNodeAdvInfo(
          nodeNameBasic   = request.mnode.meta.basic.name,
          nodeName        = request.mnode.guessDisplayNameOrIdOrQuestions,
          tfDaily         = Some(tfInfo),
          tfDaily4Ad      = tfDaily4AdOpt,
          meta            = request.mnode.meta.public,
          gallery         = gallery,
        )

        // Сериализовать и отправить ответ.
        Ok( Json.toJson(m) )
      }
    }
  }



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
            bill2Util.mOrders.countByContractId(contractId)
          }
        }
      }

      // На след.шагах нужно множество id'шников ордеров...
      val orderIdsFut = for (orders <- ordersFut) yield {
        val r = orders
          .toIdIter[Gid_t]
          .to( Set )
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
        implicit val ctxData = CtxData.jsInitTargetsAppendOne( MJsInitTargets.LkCartPageForm )(lkCtxData)
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

      // Check, if cart is payble, so view will also render scripts-prefetching tags to boost possible payment procedure:
      val isCart = requestOrderIdOpt.isEmpty
      val isCartPayableFut = if (!isCart) {
        Future.successful(false)
      } else {
        for {
          orderIdOpt  <- orderIdOptFut
          cartHasItems <- orderIdOpt.fold( Future.successful(false) ) { orderId =>
            slick.db.run {
              bill2Util.orderHasItems( orderId )
            }
          }
        } yield {
          cartHasItems
        }
      }

      // Render CSP HTTP header for possible embed-on-page payment-systems:
      val cspHeaderOpt = MPaySystems
        .values
        .iterator
        .flatMap( _.orderPageCsp )
        .reduceLeftOption( _ andThen _ )
        .flatMap( cspUtil.mkCustomPolicyHdr )

      // Данные формы для инициализации.
      for {
        orderIdOpt      <- orderIdOptFut
        formInitB64     <- formInitB64Fut
        ctx             <- ctxFut
        isCartPayable   <- isCartPayableFut
      } yield {
        import cspUtil.Implicits._

        val html = OrderPageTpl(
          orderIdOpt    = orderIdOpt,
          mnode         = request.mnode,
          formStateB64  = formInitB64,
          isCartPayable = isCartPayable,
        )(ctx)

        Ok( html )
          .withCspHeader( cspHeaderOpt )
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
            bill2Util.mItems.findByOrderId( orderId )
          }
        }
    }

    // Собрать транзакции, если это НЕ ордер-корзина:
    val mTxnsCurFut = morderOpt
      // Do not return possible cancelled txns for cart-order, because this is NOT rendered on client.
      .filter(_.status !=* MOrderStatuses.Draft)
      .flatMap(_.id)
      .fold [Future[(Seq[MTxn], Map[Gid_t, MCurrency])]]
        {
          LOGGER.trace(s"$logPrefix Txns skipping, because of order_id#${morderOpt.flatMap(_.id).orNull} or order_status#${morderOpt.map(_.status).orNull}")
          Future.successful((Nil, Map.empty))
        }
        {orderId =>
          slick.db.run {
            bill2Util.getOrderTxnsWithCurrencies( orderId )
          }
        }

    val ctx = implicitly[Context]

    // Надо добавить ценники к найденным транзакциям.
    val mTxnsPricedFut = for {
      (txnsCur, balancesCurrency) <- mTxnsCurFut
    } yield {
      val txnsPriced = (for {
        mtxn <- txnsCur.iterator
        mcurrency <- {
          val currOpt = balancesCurrency.get( mtxn.balanceId )
          if (currOpt.isEmpty) LOGGER.error(s"$logPrefix Cannot find currency for txn#${mtxn.id.orNull} balance#${mtxn.balanceId}")
          currOpt
        }
      } yield {
        val price0 = MPrice( mtxn.amount, mcurrency )
        MTxnPriced(
          txn   = mtxn.toClientSide,
          price = TplDataFormatUtil.setFormatPrice( price0 )(ctx)
        )
      })
        .to( List )

      LOGGER.trace(s"$logPrefix Found ${txnsPriced.length} txns: [${txnsPriced.iterator.flatMap(_.txn.id).mkString(",")}]")
      txnsPriced
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

    import esModel.api._

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
      nodesMap <- mNodes.multiGetMapCache( nodeIds )
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
      (for {
        mnode  <- rcvrsIds.iterator.flatMap( allNodesMap.get )
        if mnode.id.nonEmpty
      } yield {
        MSc3IndexResp(
          nodeId  = mnode.id,
          ntype   = Some( mnode.common.ntype ),
          // mnode.meta.colors,  // TODO Надо ли цвета рендерить?
          colors  = MColors.empty,
          name    = mnode.guessDisplayNameOrId,
          // Пока без иконки. TODO Решить, надо ли иконку рендерить?
        )
      })
        .toSeq
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
      adNodes = allNodesMap
        .view
        .filterKeys( itemNodeIds.contains )
        .valuesIterator
        .filter { mnode =>
          // Если не отфильтровать только карточки (могут быть и adn-узлы), то зафейлится jd-рендер.
          (mnode.common.ntype ==* MNodeTypes.Ad) &&
            mnode.extras.doc.nonEmpty
        }
        .flatMap { mad =>
          val r = for {
            doc <- mad.extras.doc
            mainRes <- doc.template.getMainBlockOrFirst()
          } yield {
            mainRes -> mad
          }

          if (r.isEmpty)
            LOGGER.warn(s"$logPrefix Ad#${mad.idOrNull} missing main-block in jd-tree: ${mad.extras.doc}")

          r
        }
        .to( LazyList )

      // Начинаем рендерить
      jdConf = MJdConf.simpleMinimal
      adDatas <- Future.traverse( adNodes ) { case ((mainTpl, mainBlkIndex), mad) =>
        // Для ускорения рендера - каждую карточку отправляем сразу в фон:
        Future {
          // Убрать wide-флаг в main strip'е, иначе будет плитка со строкой-дыркой.
          val mainNonWideTpl = jdAdUtil.resetBlkWide( mainTpl )
          val edges2 = jdAdUtil.filterEdgesForTpl(mainNonWideTpl, mad.edges)

          jdAdUtil
            .mkJdAdDataFor
            .show(
              nodeId        = mad.id,
              nodeEdges     = edges2,
              tpl           = mainNonWideTpl,
              // Тут по идее надо четверть или половину, но с учётом плотности пикселей можно округлить до 1.0. Это и нагрузку снизит.
              jdConf        = jdConf,
              allowWide     = false,
              selPathRev    = mainBlkIndex :: List.empty,
              nodeTitle     = None,
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
      mitems.map( MItem.price.modify( TplDataFormatUtil.setFormatPrice(_)(ctx) ) )
    }

    // Отбираем adn-узлы, которые заявлены в item.nodeId, рендерим в props+logo, объединяем с отрендеренными ресиверами
    val itemAdnNodesFut = for {

      itemNodeIds <- itemNodeIdsFut
      allNodesMap <- allNodesMapFut

      // Оставляем только adn-узлы.
      adnNodes = allNodesMap
        .view
        .filterKeys( itemNodeIds.contains )
        // Лениво фильтруем в immutable-коллекцию:
        .valuesIterator
        // Здесь интересны только adn-узлы:
        .filter { mnode =>
          mnode.common.ntype ==* MNodeTypes.AdnNode
        }
        .toSeq

      // Прогоняем через рендер базовых данных по узлам:
      adnNodesPropsShapes <- {
        advGeoRcvrsUtil.nodesAdvGeoPropsSrc(
          nodesSrc = Source( adnNodes ),
          // Т.к. интересует вертикальный/квадратный лого, то делаем приоритет на картинку приветствия.
          wcAsLogo = true,
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

    // Detect possible pay methods:
    val payableViasFut = for {
      morderOpt     <- morderOptFut
    } yield {
      canSubmitCart.getPayableVias( morderOpt )
    }

    // Наконец, сборка результата:
    for {
      morderOpt     <- morderOptFut
      mitems        <- mitems2Fut
      mTxnsPriced   <- mTxnsPricedFut
      itemAdnNodes  <- itemAdnNodesFut
      jdAdDatas     <- jdAdDatasFut
      orderPrices   <- orderPricesFut
      payableVias   <- payableViasFut
    } yield {
      LOGGER.trace(s"$logPrefix order#${morderOpt.flatMap(_.id).orNull}, ${mitems.length} items, ${mTxnsPriced.length} txns, ${itemAdnNodes.length} adn-nodes, ${jdAdDatas.size} jd-ads, payable via ${payableVias.size} methods")
      MOrderContent(
        order       = morderOpt,
        items       = mitems,
        txns        = mTxnsPriced,
        adnNodes    = itemAdnNodes,
        adsJdDatas  = jdAdDatas,
        orderPrices = orderPrices,
        payableVia  = payableVias,
      )
    }
  }


  /** Unholding order action. */
  def unHoldOrder( orderId: Gid_t ) = csrf.Check {
    canSubmitCart.canUnholdOrder( orderId ).async { implicit request =>
      lazy val logPrefix = s"unHoldOrder($orderId):"
      for {
        (order2, txnsCancelled) <- slick.db.run {
          bill2Util.unholdOrder( orderId )
        }

        // Cancel remote pending/opened transaction in background
        _ = Future.sequence {
          (for {
            txn <- txnsCancelled.iterator
            paySystem <- txn.paySystem.iterator
            paySysUid <- txn.psTxnUidOpt.iterator
            // Decode transaction metadata into valid query-string args for cartSubmit() action:
            submitQsOpt = txn.metadata
              .flatMap( _.asOpt[MCartSubmitQs] )
              .iterator
            cancelFs = {
              paySystem match {
                case MPaySystems.YooKassa =>
                  (for {
                    submitQs <- submitQsOpt
                    ykProfile <- yooKassaUtil.findProfile( submitQs.payVia )
                  } yield {
                    LOGGER.debug(s"$logPrefix Txn#${txn.id.orNull} paySystem#$paySystem#$paySysUid => cartSubmit qs=$submitQs => profile#$ykProfile")
                    yooKassaUtil.earlyCancelPayment( ykProfile, paySysUid )
                  })
                    .to( List )
                case _ =>
                  LOGGER.trace(s"$logPrefix Non-cancelable txn#${txn.id.orNull} because paySystem#$paySystem#$paySysUid")
                  Nil
              }
            }
            cancelFut <- cancelFs
          } yield {
            cancelFut.transform {
              case Success(_) =>
                LOGGER.trace(s"$logPrefix Cancelled ok remote txn#${txn.id.orNull} paySys#$paySystem#$paySysUid")
                Success( 1 )
              case Failure(_) =>
                LOGGER.warn(s"$logPrefix Failed to cancel remote payment: txn#${txn.id.orNull} => paySys#$paySystem#$paySysUid. See pay-system related logs.")
                Success( 0 )
            }
          })
            .to( List )
        }

        // Return updated order:
        orderContents2 <- {
          LOGGER.debug(s"$logPrefix Unholded order#$orderId per user#${request.user.personIdOpt.orNull} request. New order status is ${order2.status}.")
          _getOrderContents( Some(order2) )
        }
      } yield {
        Ok( Json.toJson( orderContents2 ) )
      }
    }
  }


  /**
    * Сабмит формы подтверждения корзины.
    *
    * @return Редирект или страница оплаты.
    */
  def cartSubmit(qs: MCartSubmitQs) = csrf.Check {
    canSubmitCart( qs, U.PersonNode, U.ContractId ).async { implicit request =>
      import slick.profile.api._

      lazy val logPrefix = s"cartSubmit()#${System.currentTimeMillis()}:"

      // Если цена нулевая, то контракт оформить как выполненный. Иначе -- заняться оплатой.
      // Чтение ордера, item'ов, кошелька, etc и их возможную модификацию надо проводить внутри одной транзакции.
      for {
        personNode  <- request.user.personNodeFut
        userContract <- bill2Util.ensureNodeContract(personNode, request.user.mContractOptFut)
        contractId  = userContract.contract.id.get

        // Дальше надо бы делать транзакцию
        // Произвести чтение, анализ и обработку товарной корзины:
        (cartResolution, mdrNotifyCtx, cartWithItems0) <- slick.db.run {
          (for {
            // Прочитать текущую корзину
            cart0   <- bill2Util.prepareCartTxn( contractId )

            // Узнать, потребуется ли уведомлять модеров по email при успешном завершении транзакции.
            mdrNotifyCtx0 <- mdrUtil.mdrNotifyPrepareCtx

            // На основе наполнения корзины нужно выбрать дальнейший путь развития событий:
            cartIdea0 <- bill2Util.maybeExecuteOrder(cart0)
          } yield {
            // Сформировать результат работы экшена
            (cartIdea0, mdrNotifyCtx0, cart0)
          })
            .transactionally
        }

        resp <- {
          LOGGER.debug(s"$logPrefix Done, person#${request.user.personIdOpt.orNull} contract#$contractId order#${cartWithItems0.order.id.orNull} with ${cartWithItems0.items.size} items\n => ${cartResolution.getClass.getSimpleName}")
          implicit lazy val ctx = implicitly[Context]
          cartResolution.idea match {

            // Need to activate an external payment system:
            case MCartIdeas.NeedMoney =>
              val paySystem = MPaySystems.default
              val cartOrder = cartResolution.newCart getOrElse cartWithItems0
              paySystem match {
                case MPaySystems.YooKassa =>
                  val payPrices = cartResolution.needMoney.get
                  require(payPrices.lengthIs == 1, s"Only one currency allowed for $paySystem")
                  val Seq(payPrice) = payPrices
                  val ykProfile = yooKassaUtil
                    .findProfile( qs.payVia )
                    .get

                  for {
                    paymentStarted <- yooKassaUtil.preparePayment(
                      // TODO payProfile: Detect test/prod using URL qs args, filtering by request.user.isSuper
                      profile   = ykProfile,
                      orderItem = cartOrder,
                      payPrice  = payPrice,
                      personOpt = Some( personNode ),
                    )(ctx)
                    // Store started not-yet-completed transaction:
                    _ <- slick.db.run {
                      (for {
                        userBalance0 <- bill2Util.ensureBalanceFor( contractId, payPrice.currency )
                        txn <- bill2Util.openPaySystemTxn(
                          balanceId   = userBalance0.id.get,
                          payAmount   = payPrice.amount,
                          orderIdOpt  = cartOrder.order.id,
                          psTxnId     = paymentStarted.payment.id,
                          paySystem   = paySystem,
                          txnMetadata = Option.when( qs.nonEmpty )( Json.toJsObject( qs ) ),
                        )
                        // Ensure, that cart order contract is same as user contractId.
                        orderHold <- bill2Util.holdOrder( cartOrder.order )
                      } yield {
                        LOGGER.trace(s"$logPrefix Hold order#${orderHold.id.orNull}, hold pending txn#${txn.id}, will await payment confirmation.")
                        None
                      })
                        .transactionally
                    }
                  } yield {
                    MCartSubmitResult(
                      cartIdea  = cartResolution.idea,
                      pay = Some( MCartPayInfo(
                        paySystem,
                        metadata = Some( paymentStarted.metadata ),
                        prefmtFooter = yooKassaUtil.prefmtFooter( ykProfile ),
                      )),
                    )
                  }

                // Deprecated. Support will be removed and not implemented here.
                case MPaySystems.YaKa =>
                  throw new UnsupportedOperationException("Deprecated, won't be implemented.")
              }

            // Order was closed, using user's internal balance.
            case MCartIdeas.OrderClosed =>
              // In background, generate need-moderation notification to related moderators.
              mdrUtil.maybeSendMdrNotify(
                mdrNotifyCtx  = mdrNotifyCtx,
                orderId       = cartWithItems0.order.id,
                personNodeFut = request.user.personNodeOptFut,
                paidTotal = MPrice
                  .toSumPricesByCurrency(
                    cartResolution.newCart
                      .iterator
                      .flatMap(_.items)
                  )
                  .values
                  .headOption,
              )(ctx)

              // TODO mdrUtil.paymentNotifyPayer( request.user.personNodeOptFut, cartWithItems0.order.id, onNodeId??? )(ctx)

              // Response to client-side js.
              val resp = MCartSubmitResult(
                cartIdea = cartResolution.idea,
              )
              Future.successful( resp )


            // У юзера оказалась пустая корзина. Отредиректить в корзину с ошибкой.
            case MCartIdeas.NothingToDo =>
              Future successful MCartSubmitResult(
                cartIdea = cartResolution.idea,
              )

          }
        }

      } yield {
        Ok( Json.toJsObject(resp) )
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
          bill2Util.mItems.deleteById( itemIds.items: _* )
        }

        // Получить обновлённые данные ордера-корзины:
        orderContents <- _getOrderContents( None )

      } yield {
        LOGGER.trace(s"cartDeleteItems(${itemIds.items.mkString(", ")}): Deleted $deletedCount items.")
        Ok( Json.toJson(orderContents) )
      }
    }
  }


  def paySystemEventGet(paySystem: MPaySystem, restPath: String) = _paySystemEvent(HttpVerbs.GET, paySystem, restPath)
  def paySystemEventPost(paySystem: MPaySystem, restPath: String) = _paySystemEvent(HttpVerbs.POST, paySystem, restPath)

  /** API REST-point for incoming notifications from external payment system. */
  private def _paySystemEvent(method: String, paySystem: MPaySystem, restPath: String) = {
    ignoreAuth().async { implicit request =>
      paySystem match {
        // YooKassa notifications:
        case MPaySystems.YooKassa =>
          yooKassaUtil.handleIncomingHttpRequest(method, restPath)

        case other =>
          NotFound("Payment System not supported: " + other)
      }
    }
  }

}
