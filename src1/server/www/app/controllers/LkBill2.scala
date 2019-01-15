package controllers

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import io.suggest.adv.info.{MNodeAdvInfo, MNodeAdvInfo4Ad}
import io.suggest.bill.cart.{MCartConf, MCartInit, MOrderContent}
import io.suggest.bill.{MCurrency, MPrice}
import io.suggest.color.MColors
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.i18n.MsgCodes
import io.suggest.init.routed.MJsInitTargets
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.order.{MOrder, MOrderStatuses, MOrders}
import io.suggest.mbill2.m.txn.{MTxn, MTxnPriced}
import io.suggest.media.{MMediaInfo, MMediaTypes}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.model.play.qsb.QsbSeq
import io.suggest.pick.PickleUtil
import io.suggest.primo.id.OptId
import io.suggest.req.ReqUtil
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import javax.inject.{Inject, Singleton}
import models.mbill._
import models.mcal.MCalendars
import models.mctx.Context
import models.mdr.MMdrNotifyMeta
import models.mproj.ICommonDi
import models.req._
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
import views.html.lk.billing._
import views.html.lk.billing.order._

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
                          esModel                     : EsModel,
                          mNodes                      : MNodes,
                          tfDailyUtil                 : TfDailyUtil,
                          mCalendars                  : MCalendars,
                          galleryUtil                 : GalleryUtil,
                          advUtil                     : AdvUtil,
                          reqUtil                     : ReqUtil,
                          isAuth                      : IsAuth,
                          nodesUtil                   : NodesUtil,
                          canViewOrder                : CanViewOrder,
                          canAccessItem               : CanAccessItem,
                          canViewNodeAdvInfo          : CanViewNodeAdvInfo,
                          isNodeAdmin                 : IsNodeAdmin,
                          advGeoRcvrsUtil             : AdvGeoRcvrsUtil,
                          jdAdUtil                    : JdAdUtil,
                          mItems                      : MItems,
                          mdrUtil                     : MdrUtil,
                          bill2Util                   : Bill2Util,
                          mOrders                     : MOrders,
                          override val mCommonDi      : ICommonDi
                        )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._

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
      val iter = for {
        mnode  <- rcvrsIds.iterator.flatMap( allNodesMap.get )
        if mnode.id.nonEmpty
      } yield {
        MSc3IndexResp(
          nodeId  = mnode.id,
          ntype   = mnode.common.ntype,
          // mnode.meta.colors,  // TODO Надо ли цвета рендерить?
          colors  = MColors.empty,
          name    = mnode.guessDisplayNameOrId,
          // Пока без иконки. TODO Решить, надо ли иконку рендерить?
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
      lazy val logPrefix = s"cartSubmit($onNodeId)#${System.currentTimeMillis()}:"

      // Если цена нулевая, то контракт оформить как выполненный. Иначе -- заняться оплатой.
      // Чтение ордера, item'ов, кошелька, etc и их возможную модификацию надо проводить внутри одной транзакции.
      for {
        personNode  <- request.user.personNodeFut
        enc         <- bill2Util.ensureNodeContract(personNode, request.user.mContractOptFut)
        contractId  = enc.mc.id.get

        // Дальше надо бы делать транзакцию
        // Произвести чтение, анализ и обработку товарной корзины:
        (cartIdea, mdrNotifyCtx, owi) <- slick.db.run {
          import slick.profile.api._
          val dbAction = for {
            // Прочитать текущую корзину
            cart0   <- bill2Util.prepareCartTxn( contractId )

            // Узнать, потребуется ли уведомлять модеров по email при успешном завершении транзакции.
            mdrNotifyCtx0 <- mdrUtil.mdrNotifyPrepareCtx(cart0)

            // На основе наполнения корзины нужно выбрать дальнейший путь развития событий:
            cartIdea0 <- bill2Util.maybeExecuteOrder(cart0)
          } yield {
            // Сформировать результат работы экшена
            (cartIdea0, mdrNotifyCtx0, cart0)
          }
          // Форсировать весь этот экшен в транзакции:
          dbAction.transactionally
        }

      } yield {
        LOGGER.debug(s"$logPrefix Done, person#${request.user.personIdOpt.orNull} contract#$contractId order#${owi.morder.id.orNull} with ${owi.mitems.size} items\n => ${cartIdea.getClass.getSimpleName}")

        cartIdea match {

          // Недостаточно бабла на балансах юзера в sio, это нормально.
          case r: MCartIdeas.NeedMoney =>
            Redirect( controllers.pay.routes.PayYaka.payForm(r.cart.morder.id.get, onNodeId) )

          // Хватило денег на балансах или они не потребовались. Такое бывает в т.ч. после возврата юзера из платежной системы.
          // Ордер был исполнен вместе с его наполнением.
          case oc: MCartIdeas.OrderClosed =>
            // Запустить уведомление по модерации.
            if (mdrUtil.isMdrNotifyNeeded( mdrNotifyCtx )) {
              for {
                personNameOpt <- nodesUtil.getPersonName( request.user.personNodeOptFut )
                orderTotal = MPrice.toSumPricesByCurrency(oc.cart.mitems).values.headOption
                tplArgs = MMdrNotifyMeta(
                  // Вычислить общую суммы обработанного заказа.
                  paidTotal   = orderTotal,
                  orderId     = owi.morder.id,
                  personId    = request.user.personIdOpt,
                  personName  = personNameOpt
                )
                _ <- mdrUtil.sendMdrNotify( mdrNotifyCtx, tplArgs )
              } {
                // Do nothing
                LOGGER.trace(s"$logPrefix Mdr-notify done, personName=${personNameOpt.orNull}, orderTotal=${orderTotal.orNull}")
              }
            }
            // Отправить юзера на страницу "Спасибо за покупку"
            Redirect( routes.LkBill2.thanksForBuy(onNodeId) )

          // У юзера оказалась пустая корзина. Отредиректить в корзину с ошибкой.
          case MCartIdeas.NothingToDo =>
            implicit val ctx = implicitly[Context]
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
