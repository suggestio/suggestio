package controllers.cbill

import controllers.SioController
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.MEsUuId
import io.suggest.mbill2.m.balance.MBalance
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.{IMOrders, MOrder}
import io.suggest.model.common.OptId
import io.suggest.util.logs.IMacroLogs
import models.mbill._
import util.acl._
import util.billing.IBill2UtilDi
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
  with ICanViewOrder
{

  import mCommonDi._


  /** Сколько ордеров рисовать на одной странице списка ордеров? */
  private def ORDERS_PER_PAGE = 10


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

    // Посчитать текущую стоимость заказа:
    val orderPricesFut = slick.db.run {
      bill2Util.getOrderPrices(orderId)
    }

    // Собрать карту балансов по id. Она нужна для рендера валюты транзакции. Возможно ещё для чего-либо пригодится.
    // Нет смысла цеплять это об необязательно найденную транзакцию, т.к. U.Lk наверху гарантирует, что mBalancesFut уже запущен на исполнение.
    val mBalsMapFut = for {
      mBals <- request.user.mBalancesFut
    } yield {
      OptId.els2idMap[Gid_t, MBalance](mBals)
    }

    val lkCtxDataFut = request.user.lkCtxDataFut

    // Отрендерить ответ, когда всё будет готово.
    for {
      txns          <- txnsFut
      orderPrices   <- orderPricesFut
      mBalsMap      <- mBalsMapFut
      lkCtxData     <- lkCtxDataFut
    } yield {
      implicit val lkCtxData1 = lkCtxData

      val tplArgs = MShowOrderTplArgs(
        mnode         = request.mnode,
        morder        = request.morder,
        orderPrices   = orderPrices,
        txns          = txns,
        balances      = mBalsMap
      )
      Ok( ShowOrderTpl(tplArgs) )
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

}
