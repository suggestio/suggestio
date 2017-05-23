package util.billing

import java.time.{Duration, OffsetDateTime}

import com.google.inject.{Inject, Singleton}
import io.suggest.bill._
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.bin.BinaryUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.dbg.{MDbgKeys, MDebug, MDebugs}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.mbill2.m.item.{IItem, IMItem, MItem, MItems}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.txn.{MTxn, MTxnTypes, MTxns}
import io.suggest.mbill2.util.effect._
import io.suggest.model.n2.node.MNodes
import io.suggest.pay.MPaySystem
import io.suggest.primo.id.OptId
import io.suggest.util.logs.MacroLogsImpl
import models.mbill.MCartIdeas
import models.mproj.ICommonDi
import models.MNode
import models.adv.geo.cur.AdvGeoShapeInfo_t
import slick.sql.SqlAction

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.15 13:49
  * Description: Утиль для биллинга второго поколения, с ордерами и корзинами.
  *
  * В терминах этого билллинга, корзина -- это черновой ордер (Draft). Возможно, ещё не созданный,
  * или уже созданный, или даже с привязанными item'ами.
  *
  * Есть ещё корзина суперюзера: всегда закрытый ордер у суперюзера, к которому по мере добавления su-бесплатных
  * операций подцепляются всё новые и новые не-Draft item'ы. Никакого смысла такая корзина не несёт,
  * и обычно скрыта от глаз. Использованные item'ы такого ордера стирать без проблем.
  */
@Singleton
class Bill2Util @Inject() (
  mOrders                         : MOrders,
  mContracts                      : MContracts,
  mItems                          : MItems,
  mBalances                       : MBalances,
  mTxns                           : MTxns,
  mNodes                          : MNodes,
  mDebugs                         : MDebugs,
  val mCommonDi                   : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._
  import slick.profile.api._


  /** id узла, на который должна сыпаться комиссия с этого биллинга. */
  val CBCA_NODE_ID: String = {
    val ck = "bill.cbca.node.id"
    val res = configuration
      .getString(ck)
      .getOrElse {
        val r = "-vr-hrgNRd6noyQ3_teu_A"
        LOGGER.debug("CBCA node id defaulted to " + r)
        r
      }
    // Проверить в фоне, существует ли узел.
    for (_ <- mNodesCache.getById(res).map(_.get).failed) {
      // Что-то пошло не так, надо застрелиться, ругнувшись в логи.
      LOGGER.error(s"CBCA NODE[$res] IS MISSING! Billing will work wrong, giving up. Check conf.key: $ck")
      mCommonDi.actorSystem.terminate()
    }
    // Вернуть id узла.
    res
  }

  /** Через сколько времени считать ордер повисшим и разворачивать его назад в ордер-корзину? */
  private val RELEASE_HOLD_ORDERS_AFTER_HOURS: Int = {
    configuration.getInt("billing.orders.release.hold.after.hours")
      .getOrElse(24)
  }

  private def MAX_STALLED_HOLDED_ORDERS_PER_ITERATION = 30

  private def _getDaysCountFix(days0: Int) = {
    Math.max(1, days0) + 1
  }


  /** Минимальный разрешённый платёж для платежной системы.
    *
    * @param paySys Платежная система.
    * @param orderPrice Стоимость заказа.
    * @return None, если минимальная цена не важна.
    *         Some(price), когда минимальная цена должна определять размер платежа.
    */
  def minPayPrice(paySys: MPaySystem, orderPrice: MPrice): Option[MPrice] = {
    def logPrefix = s"minPayPrice($paySys, $orderPrice):"
    val pp = paySys.supportedCurrency( orderPrice.currency )
      // Если валюта не поддерживается в ПС, то значит уже что-то не так, и продолжать смысла нет.
      .get

    val sioCurrencyMinAmount = orderPrice.currency.sioPaymentAmountMin
    val finalMinAmount = pp.lowerDebtLimitOpt
      .fold(sioCurrencyMinAmount) { Math.max(_, sioCurrencyMinAmount) }

    if (orderPrice.amount < finalMinAmount) {
      val minPrice = orderPrice.withAmount( finalMinAmount )
      LOGGER.debug(s"$logPrefix Current price too low, returning minPrice = $minPrice")
      Some( minPrice )
    } else {
      LOGGER.trace(s"$logPrefix Current price is enought.")
      None
    }
  }


  /** Когда ордер нормально закрыт, какой статус выставлять item'у указанного типа?. */
  private def orderClosedItemStatus( iType: MItemType ): MItemStatus = {
    if (iType == MItemTypes.BalanceCredit) {
      // Это пополнение собственного баланса. Проходит без промежуточных шагов и сразу же закрывается.
      MItemStatuses.Finished

    } else if (iType.isAdv) {
      // Пока все item'ы -- это adv-itemы. Поэтому они отправляются на модерацию.
      MItemStatuses.AwaitingMdr

    } else {
      // Should never happen. Возможно, появился какой-то новый тип item'а, для которого забыли
      // заимплементить логику в этом модуле биллинга.
      throw new UnsupportedOperationException(s"Item type '$iType' not supported for orderClosing.")
    }
  }


  /** Статус item'а при его подтверждении.
    * Подтверждение наступает, когда, например, модератор подтвердил размещение.
    *
    * @param iType Тип item'а.
    * @return Статус item'а.
    */
  private def approvedItemStatus( iType: MItemType ): MItemStatus = {
    if (iType.isApprovable) {
      // После аппрува, item рекламных размещений можно переводить в оффлайн.
      MItemStatuses.Offline
    } else {
      // Should never happen.
      // item'ы, запрещающие стадию аппрува для себя, не должны сюда попадать.
      throw new UnsupportedOperationException(s"Item type '$iType' not supported for itemApprove.")
    }
  }


  /** Посчитать кол-во дней размещения для указанного Duration. */
  def getDaysCount(dur: Duration): Int = {
    _getDaysCountFix( dur.toDays.toInt )
  }

  def cbcaNodeOptFut = mNodesCache.getById(CBCA_NODE_ID)

  sealed case class EnsuredNodeContract(mc: MContract, mnode: MNode)


  /**
    * Вернуть контракт для узла. Если нет контракта, то создать и вернуть обновлённый узел.
    *
    * @param mnode Исходный узел, обычно person.
    * @param mcOptFut Фьючерс с подсказкой контракта.
    * @return
    */
  def ensureNodeContract(mnode: MNode, mcOptFut: Future[Option[MContract]]): Future[EnsuredNodeContract] = {
    mcOptFut
      .map(_.get)
      // Проверить, является ли контракт-подсказка связанным с узлом.
      .filter { mc =>
        val ncIdOpt = mnode.billing.contractId
        val res = ncIdOpt == mc.id
        if (!res)
          LOGGER.warn(s"ensureNodeContract(): Unrelated contract[${mc.id}] passed as hint for node[${mnode.idOrNull}]. Expected node's contract is [$ncIdOpt].")
        res
      }
      .map { mc =>
        EnsuredNodeContract(mc, mnode)
      }
      .recoverWith { case _: NoSuchElementException =>
        ensureNodeContract(mnode)
      }
  }

  /**
    * Найти и вернуть контракт для указанного id.
    * Если контракт не найден или не сущесвует, то он будет создан.
    *
    * @param mnode Узел.
    * @return Фьючерс с экземпляром MContract.
    */
  def ensureNodeContract(mnode: MNode): Future[EnsuredNodeContract] = {
    lazy val logPrefix = s"ensureNodeContract(${mnode.idOrNull}):"

    // Поискать связанный контракт, если есть.
    val mcOptFut = FutureUtil.optFut2futOpt( mnode.billing.contractId ) { contractId =>
      // Запрашиваем сохраненный контракт узла из модели.
      val fut = slick.db.run {
        mContracts.getById(contractId)
      }
      fut.onSuccess { case None =>
        // should never happen
        LOGGER.warn(s"$logPrefix Contract[$contractId] is missing, but is saved into node.billing.")
      }
      fut
    }

    // Если контракт есть, то собрать контейнер результат метода:
    val res0Fut = for (mc0 <- mcOptFut) yield {
      EnsuredNodeContract(mc0.get, mnode)
    }

    // Отработать случай, когда контракта нет.
    res0Fut.recoverWith { case _: NoSuchElementException =>
      // Контракт не найден, значит нужно создать новый, сохранить везде и вернуть.
      for {
        // Создать новый контракт в БД биллинга
        mc2 <- {
          val mc = MContract()
          slick.db.run {
            mContracts.insertOne(mc)
          }
        }

        // Сохранить id свежесозданного контракта в текущую ноду
        mnode2 <- {
          val updFut = mNodes.tryUpdate(mnode) { mnode0 =>
            mnode0.copy(
              billing = mnode0.billing.copy(
                contractId = mc2.id
              )
            )
          }

          // В фоне среагировать на завершение обновления узла.
          updFut.onComplete {
            // Всё хорошо, тихо залоггировать.
            case _: Success[_] =>
              LOGGER.debug(s"$logPrefix Initialized new contract[${mc2.id}] for node.")
            // Не удалось сохранить contract_id в ноду, откатить свежесозданный ордер
            case _: Failure[_] =>
              for (id <- mc2.id) {
                slick.db.run {
                  mOrders.deleteById(id)
                }
              }
              LOGGER.error(s"$logPrefix Rollback contact[${mc2.id}] init, because unable to update MNode.")
          }

          updFut
        }

      } yield {
        EnsuredNodeContract(mc2, mnode2)
      }
    }
  }

  /** Поиск ордера-корзины. */
  def getCartOrder(contractId: Gid_t) = getLastOrder(contractId, MOrderStatuses.Draft)

  /** Поиск id ордера-корзины. */
  def getCartOrderId(contractId: Gid_t): DBIOAction[Option[Gid_t], NoStream, Effect.Read] = {
    _getLastOrderSql(contractId, MOrderStatuses.Draft)
      .map(_.id)
      .result
      .headOption
  }

  private def _getLastOrderSql(contractId: Gid_t, status: MOrderStatus) = {
    mOrders.query
      .filter { q =>
        (q.contractId === contractId) && (q.statusStr === status.strId)
      }
      .sortBy(_.id.desc.nullsLast)
      .take(1)
  }

  /** Поиск последнего ордера указанного контракта и с указанным статусом. */
  def getLastOrder(contractId: Gid_t, status: MOrderStatus): SqlAction[Option[MOrder], NoStream, Effect.Read] = {
    _getLastOrderSql(contractId, status)
      .result
      .headOption
  }


  /** Попытаться удалить ордер, если есть id. */
  def maybeDeleteOrder(orderIdOpt: Option[Gid_t]): DBIOAction[Int, NoStream, RWT] = {
    orderIdOpt.fold [DBIOAction[Int, NoStream, RWT]] {
      LOGGER.trace("maybeClearCart(): orderId is empty, skipping")
      DBIO.successful(0)
    } { deleteOrder }
  }
  /** Удалить ордер вместе с item'ами. Такое можно сделать только если не было транзакций по ордеру/item'ам. */
  def deleteOrder(orderId: Gid_t): DBIOAction[Int, NoStream, RWT] = {
    val dbAction = for {
      orderItemIds  <- mItems.findIdsByOrderId( orderId )
      // Удаляем все item'ы в корзине разом.
      itemsDeleted  <- mItems.deleteById(orderItemIds: _*)
      // Удаляем ордер корзины, транзакций по ней быть не должно, а больше зависимостей и нет.
      ordersDeleted <- mOrders.deleteById(orderId)
      // Удалить все дебажные данные для всех удаляемых элементов.
      debugsDeleted <- mDebugs.deleteByObjectIds( orderId +: orderItemIds )

    } yield {
      LOGGER.info(s"clearCart($orderId) cartOrderId[$orderId] cleared. Total deleted:\n $itemsDeleted items\n $ordersDeleted orders\n $debugsDeleted debugs")
      itemsDeleted
    }
    dbAction.transactionally
  }


  def deleteItem(itemId: Gid_t): DBIOAction[Int, NoStream, WT] = {
    val dbAction = for {
      itemsDeleted  <- mItems.deleteById(itemId)
      debugsDeleted <- mDebugs.deleteByObjectId( itemId )
    } yield {
      LOGGER.debug(s"deleteItem($itemId): Deleted $itemsDeleted items with $debugsDeleted debugs.")
      itemsDeleted
    }
    dbAction.transactionally
  }


  def deleteContract(contractId: Gid_t): DBIOAction[Int, NoStream, WT] = {
    val dbAction = for {
      contractsDeleted <- mContracts.deleteById( contractId )
      debugsDeleted    <- mDebugs.deleteByObjectId( contractId )
    } yield {
      LOGGER.info(s"deleteContract($contractId): Deleted $contractsDeleted contracts, $debugsDeleted debugs")
      contractsDeleted
    }
    dbAction.transactionally
  }


  /** Найти корзину и очистить её. */
  def clearCart(contractId: Gid_t): DBIOAction[Int, NoStream, RWT] = {
    val a = for {
      cartOrderOpt    <- getCartOrder(contractId)
      cartOrderIdOpt   = cartOrderOpt.flatMap(_.id)
      itemsDeleted    <- maybeDeleteOrder( cartOrderIdOpt )
    } yield {
      itemsDeleted
    }
    // Явно требуем транзакцию, чтобы избежать смены состояния items/orders во время обработки.
    a.transactionally
  }


  def ensureCart(contractId: Gid_t, status0: MOrderStatus = MOrderStatuses.Draft): DBIOAction[MOrder, NoStream, RW] = {
    getLastOrder(contractId, status0).flatMap { orderOpt =>
      ensureCartOrder(orderOpt, contractId, status0)
    }
  }


  /**
    * Убедиться, что для контракта существует ордер-корзина для покупок.
    *
    * @param contractId Номер договора.
    * @return Фьючерс с ордером корзины.
    */
  def ensureCartOrder(orderOpt: Option[MOrder], contractId: Gid_t, status0: MOrderStatus): DBIOAction[MOrder, NoStream, RW] = {
    orderOpt.fold {
      val cartOrderStub = MOrder(status0, contractId)
      val orderAct = mOrders.insertOne(cartOrderStub)
      orderAct.map { order2 =>
        LOGGER.debug(s"ensureNodeCart($contractId): Initialized new cart order[${order2.id.orNull}]")
        order2
      }
    } { DBIO.successful }
  }

  /** Нулевая цена. */
  def zeroPrice: MPrice = {
    MPrice(0.0, MCurrencies.default)
  }

  /** Нулевой прайсинг размещения. */
  def zeroPricing: MGetPriceResp = {
    val prices = zeroPrice :: Nil
    MGetPriceResp(prices)
  }
  def zeroPricingFut = Future.successful( zeroPricing )


  def getAdvPricing(prices: Iterable[MPrice]): MGetPriceResp = {
    // Если есть разные валюты, то операция уже невозможна.
    if (prices.nonEmpty) {
      MGetPriceResp(prices)
    } else {
      zeroPricing
    }
  }


  /** Найти все item'ы указанного ордера. */
  def orderItemsFut(orderId: Gid_t): Future[Seq[MItem]] = {
    slick.db.run {
      mItems.findByOrderId(orderId)
    }
  }


  def prepareOpenedItems(orderId: Gid_t): DBIOAction[Seq[MItem], Streaming[MItem], Effect.Read] = {
    mItems.findByOrderIdBuilder(orderId)
      .filter(_.statusStr === MItemStatuses.Draft.strId)
      .result
      .forUpdate
  }
  def prepareCartOrderItems(order: MOrder): DBIOAction[MOrderWithItems, NoStream, Effect.Read] = {
    val orderId       = order.id.get
    for {
      mitems        <- prepareOpenedItems(orderId)
    } yield {
      LOGGER.trace(s"prepareCartItems($orderId): cart contains ${mitems.size} items.")
      MOrderWithItems(order, mitems)
    }
  }

  /** Подготовится к транзакции внутри корзины. */
  def prepareCartTxn(contractId: Gid_t): DBIOAction[MOrderWithItems, NoStream, Effect.Read] = {
    for {
      cartOrderOpt  <- getCartOrder(contractId).forUpdate
      order         = cartOrderOpt.get
      res           <- prepareCartOrderItems(order)
    } yield {
      res
    }
  }


  /**
    * Есть ли в списке товаров хоть один, который имеет какую-то позитивную стоимость?
    *
    * @param mitems Товары для анализа стоимости.
    * @return true, если хотя бы один товар требует денег.
    */
  def itemsHasPrice(mitems: TraversableOnce[IItem]): Boolean = {
    hasPositivePrice {
      mitems.toIterator.map(_.price)
    }
  }

  /** Есть ли в списке цен хоть одна позитивная?
    *
    * @param prices Список цен для анализа.
    * @return true, если хотя бы одна цена не бесплатная.
    */
  def hasPositivePrice(prices: TraversableOnce[MPrice]): Boolean = {
    prices.exists { price =>
      price.amount > 0.0
    }
  }


  /** slick-экшен для запуска внутри транзакции для исполнения цельного ордера.
    * Считается, что все оплаты уже посчитаны, проверены и списаны.
    * Т.е. это жесткое закрытие ордера, и не всегда уместно.
    *
    * @param order Обрабатываемый ордер вместе с item'ами.
    * @return DBIO-экшен, с какими-то данными или без них.
    */
  def closeOrder(order: IOrderWithItems): DBIOAction[MOrderWithItems, NoStream, Effect.Write] = {
    for {
      // Обновляем статус ордера.
      morder3 <- {
        val morder2 = order.morder.withStatus( MOrderStatuses.Closed )
        for {
          ordersUpdated <- mOrders.saveStatus( morder2 )
          if ordersUpdated == 1
        } yield {
          morder2
        }
      }

      // Обновляем item'ы ордера, возвращаем обновлённые инстансы.
      mitems3 <- {
        val actions = for (itm <- order.mitems) yield {
          val itm2 = itm.withStatus( orderClosedItemStatus(itm.iType) )
          for {
            itemsUpdated <- mItems.updateStatus(itm2)
            if itemsUpdated == 1
          } yield {
            itm2
          }
        }
        DBIO.sequence(actions)
      }
    } yield {
      // Собрать возвращаемый результат экшена
      MOrderWithItems(morder3, mitems3)
    }
  }


  /**
    * slick-экшен для выполнения анализа ордера (обычно -- товарной корзины) и принятия дальнейшего решения.
    * Решения описаны в TxnResults.
    *
    * @param order Результат prepareCartTxn().
    */
  def maybeExecuteOrder(order: IOrderWithItems): DBIOAction[MCartIdeas.Idea, NoStream, RW] = {

    lazy val logPrefix = s"maybeExecuteCart(${order.morder.id.orNull} ${System.currentTimeMillis}):"

    LOGGER.trace(s"$logPrefix There are ${order.mitems.size} items: ${order.mitems.iterator.flatMap(_.id).mkString(", ")}")

    if (order.mitems.isEmpty) {
      // Корзина пуста. Should not happen.
      LOGGER.trace(s"$logPrefix no items in the cart")
      DBIO.successful( MCartIdeas.NothingToDo )

    } else if ( !itemsHasPrice(order.mitems) ) {
      // Итемы есть, но всё бесплатно. Исполнить весь этот контракт прямо тут.
      LOGGER.trace(s"$logPrefix Only FREE items in cart.")
      for (_ <- closeOrder(order)) yield {
        MCartIdeas.OrderClosed(order, Nil)
      }

    } else {

      // Товары есть, и они не бесплатны.
      LOGGER.trace(s"$logPrefix There are priceful items in cart.")

      for {
        // Узнаём текущее финансовое состояние юзера...
        balances    <- mBalances.findByContractId( order.morder.contractId ).forUpdate

        // Строим карту полученных балансов юзера для удобства работы:
        balancesMap = MCurrencies.hardMapByCurrency(balances)

        // Считаем полную стоимость заказа-корзины.
        totalPrices = MPrice.toSumPricesByCurrency(order.mitems).values

        // Пройтись по ценам, узнать достаточно ли бабла у юзера на балансах по всем валютам заказа
        (balUpdActions, notEnoughtPrices) = {
          val balActionsAcc = List.empty[DBIOAction[MBalance, NoStream, Effect.Write]]
          val notEnougtAcc = List.empty[MPrice]

          totalPrices.foldLeft ((balActionsAcc, notEnougtAcc)) {
            case (acc0 @ (enought0, notEnough0), totalPrice) =>
              import totalPrice.currency
              balancesMap.get( currency ).fold {
                // Пока ещё нет такой валюты на балансах юзера. Юзеру нужно оплатить всю стоимость в валюте.
                LOGGER.trace(s"$logPrefix Balance for currency $currency is missing. Guessing as zero amount.")
                (enought0, totalPrice :: notEnough0)

              } { balance0 =>
                val balAmount2 = balance0.price.amount - totalPrice.amount
                if (balAmount2 < balance0.low) {
                  // Баблишко-то вроде есть, но его не хватает. Юзеру нужно доплатить разность.
                  val needAmount = -balAmount2
                  LOGGER.trace(s"$logPrefix User needs to pay some money: $needAmount $currency")
                  val needPrice = totalPrice.copy(amount = needAmount)
                  (enought0, needPrice :: notEnough0)

                } else if (notEnough0.nonEmpty) {
                  // Нет смысла собирать экшен обновления кошелька, если уже не хватило денег в какой-то другой валюте
                  LOGGER.trace(s"$logPrefix Skip building balance action, because of notEnounghAcc.nonEmpty")
                  acc0

                } else {
                  // Баблишка на балансе хватает для оплаты покупок в текущей валюте.
                  LOGGER.trace(s"$logPrefix Enought money on balance[${balance0.id.orNull}] for totalPrice=$totalPrice.")

                  // Собрать DB-экшен обновления баланса и закинуть в аккамулятор:
                  val withdrawAmount = -totalPrice.amount
                  val dbAction = for {
                    // Записать новый баланс юзера
                    mbalance2Opt  <- mBalances.incrAmountAndBlockedBy(balance0, withdrawAmount)
                    mbalance2     = mbalance2Opt.get

                    // Добавить транзакцию списания денег с баланса юзера
                    usrTxn <- mTxns.insertOne {
                      MTxn(
                        balanceId   = mbalance2.id.get,
                        amount      = withdrawAmount,
                        txType      = MTxnTypes.Payment,
                        orderIdOpt  = order.morder.id
                      )
                    }
                  } yield {
                    LOGGER.trace(s"$logPrefix Blocked $totalPrice, updated balance is $balAmount2, txn[${usrTxn.id.orNull}].")
                    mbalance2
                  }
                  // Закинуть DB-экшен обновления баланса в аккамулятор DB-экшенов
                  (dbAction :: enought0, notEnough0)
                }
              }
          }
        }

        // На прошлом шаге были собраны данные по финансовым возможностям юзера.
        res <- {
          if (notEnoughtPrices.nonEmpty) {
            // Юзеру на балансах не хватает денег по как минимум одной валюте.
            // Вернуть наверх требование сначала доплатить сколько-то недостающих денег.
            val idea = MCartIdeas.NeedMoney(order, notEnoughtPrices)
            DBIO.successful(idea)

          } else {
            // Есть достаточно средств на балансах -- их и тратим.
            for {
              balances2 <- DBIO.sequence(balUpdActions)
              // Закрываем исходный ордер корзины.
              order2    <- closeOrder(order)
            } yield {
              MCartIdeas.OrderClosed(order2, balances2)
            }
          }
        }

      } yield {
        res
      }
    }
  }

  sealed case class ForceFinalizeOrderRes(
                                          closedOrder         : MOrder,
                                          skippedCartOpt      : Option[MOrder],
                                          newBalances         : Map[MCurrency, MBalance],
                                          okItemsCount        : Int
                                         )

  /**
    * Принудительное исполнение ордера.
    * Это похоже на успешное исполнение maybeExecuteCart(), но:
    * - Исполнение идёт поштучно, по item'ам.
    * - Если встречаются item'ы, на которые не хватает денег, то они переносятся в ордер корзины.
    * - По итогам, ордер закрывается, а на балансе блокируются только деньги, потраченные на item'ы.
    *
    * @param owi Ордер и его item'ы.
    * @return DB-экшен.
    */
  def forceFinalizeOrder(owi: IOrderWithItems): DBIOAction[ForceFinalizeOrderRes, NoStream, RWT] = {
    val orderId = owi.morder.id.get
    val contractId = owi.morder.contractId

    lazy val logPrefix = s"forceFinalizeOrder($orderId):"
    LOGGER.trace(s"$logPrefix Starting, items = ${OptId.els2ids(owi.mitems).mkString(", ")}")

    val a = for {

      // Узнаём текущее финансовое состояние юзера в контексте текущего ордера...
      balsMap0 <- {
        // Собираем валюты, которые упомянуты в item'ах заказа.
        val itemCurrencies = MCurrencies
          .toCurrenciesIter {
            MPrice.toPricesIter(owi.mitems)
          }
          .toSet
        for {
          balOpts0 <- mBalances.getByContractCurrency1(contractId, itemCurrencies)
            .forUpdate
        } yield {
          // Строим карту полученных балансов юзера для удобства работы:
          val balsMap = MCurrencies.hardMapByCurrency(balOpts0)
          LOGGER.trace(s"$logPrefix Found ${balsMap.size} balances (${balOpts0.orNull}) for ${itemCurrencies.size} currencies (${itemCurrencies.mkString(",")})")
          balsMap
        }
      }

      // Пошаговая и безопасная обработка item'ов текущего заказа.
      r <- {
        // Аккамулятор нетривиален, поэтому используем отдельный класс для аккамулятора.
        case class Acc(
                        balsMap      : Map[MCurrency, MBalance] = balsMap0,
                        dbAcc        : List[DBIOAction[_, NoStream, Effect.Write]] = Nil,
                        skipped      : List[MItem] = Nil,
                        okItemsCount : Int = 0
                      )
        // Проходим по всем item'ам, пытаемся вычесть лишку с балансов.
        val acc9 = owi.mitems
          .foldLeft( Acc() ) { (acc0, mitem) =>
            val balOpt0 = acc0.balsMap.get( mitem.price.currency )

            // Some(acc2) || None, если недостаточно средств на балансе.
            val acc2Opt = for {
              bal0          <- balOpt0
              isDebt         = mitem.iType.isDebt
              // itemAmount может быть отрицательный, если просто пополнение счёта.
              itemAmount     = mitem.price.amount
              if {
                // Кредитование (отрицательное списание) баланса всегда возможно.
                !isDebt || {
                  // Положительное списание с баланса -- только если денег на балансе достаточно.
                  val balAmount2 = bal0.price.amount - itemAmount
                  balAmount2 >= bal0.low
                }
              }

            } yield {

              // Обновить баланс юзера в текущей валюте.
              val bal2 = if (isDebt) {
                // Это обычное списание с баланса (дебет).
                val b2 = bal0.blockAmount( itemAmount )
                LOGGER.trace(s"$logPrefix Enought money for item ${mitem.id.orNull} on balance ${b2.id.orNull}. Blocked $itemAmount, new balance: $b2")
                b2
              } else {
                // Это пополнение баланса юзером (крЕдит) вместо покупки чего-либо.
                val b2 = bal0.plusAmount( itemAmount )
                LOGGER.trace(s"$logPrefix Crediting balance for item${mitem.id.orNull} by $itemAmount. New balance#${b2.id.orNull} = ${b2.price}")
                b2
              }

              // Обновить статус item'а.
              val mitem2 = mitem.withStatus( orderClosedItemStatus(mitem.iType) )

              // Сохранить обновлённый статус item'а, убедившись, что item действительно сейчас есть в таблице.
              val itmUpdDbAct = for {
                itemsUpdated <- mItems.updateStatus(mitem2)
                if itemsUpdated == 1
              } yield {
                LOGGER.trace(s"$logPrefix Item ${mitem2.id.orNull} of type ${mitem2.iType} ($itemAmount) status updated to ${mitem2.status}")
                mitem2
              }

              // Денег значит хватает. Заблокировать средства на текущем балансе и апдейтнуть текущий item.
              acc0.copy(
                balsMap       = acc0.balsMap + (bal2.price.currency -> bal2),
                dbAcc         = itmUpdDbAct :: acc0.dbAcc,
                okItemsCount  = acc0.okItemsCount + 1
              )
            }

            // Отработать случай, когда недостаточно денег или когда баланса с деньгами вообще не существует.
            acc2Opt.getOrElse {
              LOGGER.warn(s"$logPrefix Not enought money for item ${mitem.id.orNull}, need ${mitem.price}, but only have ${balOpt0.orNull}")
              acc0.copy(
                skipped = mitem :: acc0.skipped
              )
            }
          }

        lazy val skippedItemIds = OptId.els2idsSet(acc9.skipped)

        LOGGER.debug(s"$logPrefix For finalization ${acc9.okItemsCount} db actions, skipped ${skippedItemIds.size} items. Balances2 =\n ${acc9.balsMap.valuesIterator.mkString(",\n ")}")
        for {
          // Обновить item'ы.
          _ <- DBIO.seq(acc9.dbAcc: _*)   // Порядок не важен, поэтому без reverse().

          // Обновить балансы
          _ <- {
            LOGGER.trace(s"$logPrefix Will update balances, new balances are:\n${acc9.balsMap.valuesIterator.mkString(",\n ")}")
            val balsDbActs = acc9.balsMap
              .valuesIterator
              .map { bal =>
                for {
                  balsUpdated <- mBalances.saveAmountAndBlocked(bal)
                  if balsUpdated == 1
                } yield {
                  bal
                }
              }
              .toSeq
            DBIO.seq(balsDbActs: _*)
          }

          // skipped-item'ы перенести в текущий ордер-корзину.
          skippedCartOpt <- {
            if (acc9.skipped.nonEmpty) {
              // Обработка skipped-item'ов -- это нештатная ситуация. Мы получаем/создаём корзину, и перемещаем висячие ордеры туда.
              LOGGER.warn(s"$logPrefix There are ${skippedItemIds.size} items. Looks like, money on the balances was not enought to complete the order#$orderId fully. Item ids are: ${skippedItemIds.mkString(", ")}")
              for {
                // Возможна (но очень маловероятна) ситуация race-conditions, когда здесь создаётся корзина параллельно с созданием такой же корзины ещё где-то.
                // Такой исход должен отрабатываться по окончании этой транзакции новой отранзакцией, вызывающей mergeOrders().
                cartOrder <- ensureCart(contractId)
                cartOrderId = cartOrder.id.get

                // Переместить item'ы закрываемого ордера:
                itemsUpdated <- mItems.query
                  .filter(_.id inSet skippedItemIds )
                  .map(_.orderId)
                  .update(cartOrderId)

                // Переместить транзакции item'ов закрываемого ордера
                txnsUpdated <- mTxns.query
                  .filter { t =>
                    (t.itemIdOpt inSet skippedItemIds) && (t.orderIdOpt === orderId)
                  }
                  .map(_.orderIdOpt)
                  .update( Some(cartOrderId) )

              } yield {
                LOGGER.info(s"$logPrefix Moved $itemsUpdated unpaid items with $txnsUpdated item-txns from finalizing order#$orderId to cart order#$cartOrderId")
                Some(cartOrder)
              }
            } else {
              LOGGER.trace(s"$logPrefix No items skipped, nothing to move into cart-order, it's normal and good.")
              DBIO.successful(None)
            }
          }

          // Закрыть статус текущего ордера.
          morder9 <- {
            val morder2 = owi.morder.withStatus( MOrderStatuses.Closed )
            for {
              ordersUpdated <- mOrders.saveStatus( morder2 )
              if ordersUpdated == 1
            } yield {
              LOGGER.debug(s"$logPrefix Order#$orderId closed.")
              morder2
            }
          }

        } yield {
          LOGGER.trace(s"$logPrefix Finished processing order#$orderId with ${owi.mitems.size} orig.items. SkippedCartOpt=${skippedCartOpt.flatMap(_.id).orNull} (normally, null).")
          ForceFinalizeOrderRes(
            closedOrder     = morder9,
            skippedCartOpt  = skippedCartOpt,
            newBalances     = acc9.balsMap,
            okItemsCount    = acc9.okItemsCount
          )
        }
      }

    } yield {
      LOGGER.trace(s"$logPrefix Done.")
      // Вернуть что-нибудь. Результат тут пока не очень важен.
      r
    }
    a.transactionally
  }



  /** Логика заливки на баланс денег. Обычно используется для нужд валютных кошельков CBCA.
    *
    * @param contractId id контракта получателя денег (обычно CBCA).
    * @param price Общая цена пополнения в валюте.
    * @return DBIOAction создания/обновления кошелька узла.
    */
  def increaseBalanceAsIncome(contractId: Gid_t, price: MPrice): DBIOAction[MTxn, NoStream, RWT] = {
    lazy val logPrefix = s"increaseBalanceSimple($contractId,$price)[${System.currentTimeMillis()}]:"
    val dba = for {
      // Сразу искать баланс в нужной валюте:
      balOpt      <- mBalances.getByContractCurrency(contractId, price.currency).forUpdate

      // Залить средства на баланс целевого узла.
      bal2 <- balOpt.fold [DBIOAction[MBalance, NoStream, Effect.Write]] {
        val cb0 = MBalance(contractId, price)
        LOGGER.trace(logPrefix + "Initializing new balance for this currency: " + cb0)
        mBalances.insertOne(cb0)

      } { cb0 =>
        LOGGER.trace(logPrefix + "Updating existing balance " + cb0)
        for {
          cb1Opt <- mBalances.incrAmountBy(cb0, price.amount)
          cb1 = cb1Opt.get
        } yield {
          cb1
        }
      }

      // Записать транзакцию по зачислению на баланс CBCA
      ctxn2         <- {
        val ctxn0 = MTxn(
          balanceId   = bal2.id.get,
          amount      = price.amount,
          txType      = MTxnTypes.Income
        )
        LOGGER.trace(logPrefix + "Inserting txn for balance #" + bal2.id.orNull)
        mTxns.insertOne(ctxn0)
      }

    } yield {
      LOGGER.trace(s"$logPrefix: Done")
      ctxn2
    }
    dba.transactionally
  }


  /** Общий код доступа к модерируемому item'у и требующему последующего обновления. */
  private def _prepareAwaitingItem(itemId: Gid_t) = {
    for {
      mitemOpt0 <- {
        mItems
          .getByIdStatusAction(itemId, MItemStatuses.AwaitingMdr)
          .forUpdate
      }
    } yield {
      // Если нет обновляемого итема, пусть будет ошибка. Так и надо.
      mitemOpt0.get
    }
  }


  /** Получить доступ к кошель юзера, купившего указанный item. */
  private def _item2balance(mitem: IItem) = {
    for {
      // Узнать id контракта, по которому проходит данный item.
      contractIdOpt <- mOrders.getContractId(mitem.orderId)
      contractId = contractIdOpt.get

      // Прочитать текущий баланс
      balanceOpt0 <- {
        mBalances
          .getByContractCurrency(contractId, mitem.price.currency)
          .forUpdate
      }
    } yield {
      balanceOpt0.get
    }
  }


  /**
    * Подготовить получателя денег к эксплуатации.
    *
    * @param nodeId id узла-получателя денег.
    * @return Future с итогом работы.
    *         Если Future(None), то нет возможности получить данные по ресиверу.
    */
  def prepareMoneyReceiver(nodeId: String): Future[Option[EnsuredNodeContract]] = {
    // Организовать сборку данных по контракту получателя.
    val fut0 = for {
      rcvrNodeOpt <- mNodesCache.getById( nodeId )
      rcvrNode    =  rcvrNodeOpt.get
      enc         <- ensureNodeContract(rcvrNode)
    } yield {
      Some(enc)
    }
    // Ресивер денег может быть не готов к профиту, но это невероятная ситуация.
    fut0.recover { case _: NoSuchElementException =>
      LOGGER.warn(s"prepareMoneyReceiver($nodeId): Receiver node not found/not exists.")
      None
    }
  }


  /** Контейнер результата экшена аппрува item'а. */
  sealed case class ApproveItemResult(override val mitem: MItem)
    extends IMItem

  /**
    * Аппрув модерация item'а.
    * Происходит списание заблокированных денег из кошелька юзера, зачисление средств в пользу CBCA,
    * обновление item'a.
    *
    * @param itemId id обрабатываемого item'а.
    * @return Экшен с результатом работы.
    */
  def approveItem(itemId: Gid_t): DBIOAction[ApproveItemResult, NoStream, RWT] = {
    lazy val logPrefix = s"approveItemAction($itemId):"

    val a = for {
      // Получить и заблокировать обрабатываемый item.
      mitem0 <- _prepareAwaitingItem(itemId)

      // Запустить В ФОНЕ вне транзакции сбор базовой инфы о получателе денег. "mr" означает "Money Receiver".
      mrInfoOptFut: Future[Option[EnsuredNodeContract]] = {
        if (mitem0.price.amount <= 0.0) {
          Future.successful( None )
        } else {
          val mrNodeId = if (mitem0.iType.moneyRcvrIsCbca) {
            CBCA_NODE_ID

          } else {
            mitem0.rcvrIdOpt.getOrElse {
              val cbcaNodeId = CBCA_NODE_ID
              LOGGER.warn(s"$logPrefix Item has itype=${mitem0.iType}, rcvr is NOT cbca, but item.rcvrIdOpt is empty! Money receiver will be CBCA $cbcaNodeId")
              cbcaNodeId
            }
          }
          LOGGER.trace(s"$logPrefix Money receiver => $mrNodeId")
          prepareMoneyReceiver(mrNodeId)
        }
      }

      // Получить и заблокировать баланс покупателя
      balance0 <- _item2balance(mitem0)

      // Списать blocked amount с баланса покупателя
      usrAmtBlocked2 <- mBalances.incrBlockedBy(balance0.id.get, -mitem0.price.amount)

      // Запустить обновление полностью на стороне БД.
      mitem2 <- {
        LOGGER.debug(s"$logPrefix Buyer blocked balance[${balance0.id.orNull}] freed ${mitem0.price.amount}: ${balance0.blocked} => ${usrAmtBlocked2.orNull} ${balance0.price.currency}")
        val mitem1 = mitem0.copy(
          status      = approvedItemStatus( mitem0.iType ),
          dateStatus  = OffsetDateTime.now()
        )
        mItems.query
          .filter(_.id === itemId)
          .map { i =>
            (i.status, i.dateStatus)
          }
          .update((mitem1.status, mitem1.dateStatus))
          .filter(_ == 1)
          .map(_ => mitem1)
      }

      // Залить деньги получателю денег, если возможно.
      mrInfoOpt <- DBIO.from( mrInfoOptFut )
      _ <- {
        LOGGER.trace(s"$logPrefix item[${mitem2.id.orNull}] status: ${mitem0.status} => ${mitem2.status}")

        mrInfoOpt.fold [DBIOAction[Option[MBalance], NoStream, RW]] {
          LOGGER.warn(s"$logPrefix Money income skipped, so they are lost.")
          DBIO.successful(None)

        } { mrInfo =>
          // Есть получатель финансов, зачислить ему на необходимый баланс.
          val price = mitem2.price
          // TODO Отработать комиссию с item'а здесь? Если да, то откуда её брать? С тарифа узла и типа item'а?
          for {
            // Найти/создать кошелек получателя денег
            mrBalance0    <- ensureBalanceFor(mrInfo.mc.id.get, price.currency)
            // Зачислить деньги на баланс.
            // TODO В будущем, когда будет нормальная торговля между юзерами, надо будет проводить какие-то транзакции на стороне seller'а.
            // TODO И по идее надо будет зачислять селлеру как blocked, т.к. продавца надо держать на поводке.
            mrAmount2Opt  <- mBalances.incrAmountBy(mrBalance0.id.get, price.amount)
            // seller-транзакцию не создаём, т.к. она на раннем этапе не нужна: будет куча ненужного мусора в txn-таблице.
            // Возможно, транзакции потом будут храниться в elasticsearch, в т.ч. для статистики.
          } yield {
            val mrAmount2 = mrAmount2Opt.get
            LOGGER.debug(s"$logPrefix Money receiver[${mrInfo.mnode.id.orNull}] contract[${mrInfo.mc.id.orNull}] balance[${mrBalance0.id.orNull}] updated: ${mrBalance0.blocked} + ${price.amount} => $mrAmount2 ${mrBalance0.price.currency}")
            val mrBalance1 = mrBalance0.copy(
              price = mrBalance0.price.copy(
                amount = mrAmount2
              )
            )
            Some(mrBalance1)
          }
        }
      }

    } yield {
      LOGGER.trace(s"$logPrefix Done ok.")
      ApproveItemResult( mitem2 )
    }
    a.transactionally
  }


  /**
    * Найти/создать баланс для указанного контракта и валюты.
    *
    * @param contractId id контракта.
    * @param currency Валюта баланса.
    * @return Экшен, возвращающий баланс, готовый к обновлению.
    */
  def ensureBalanceFor(contractId: Gid_t, currency: MCurrency): DBIOAction[MBalance, NoStream, RW] = {
    lazy val logPrefix = s"ensureBalanceFor($contractId,$currency):"
    for {
      // Считать баланс получателя...
      balanceOpt <- {
        mBalances.getByContractCurrency(contractId, currency)
          .forUpdate
      }

      // Если баланс отсутствует, то инициализировать
      balance0 <- {
        balanceOpt.fold [DBIOAction[MBalance, NoStream, RW]] {
          LOGGER.trace(s"$logPrefix Initializing new money receiver's balance...")
          mBalances.initByContractCurrency(contractId, currency)
        } { DBIO.successful }
      }

    } yield {
      balance0
    }
  }


  /** Результат исполнения экшена refuseItemAction(). */
  sealed case class RefuseItemResult(override val mitem: MItem, mtxn: MTxn)
    extends IMItem

  /**
    * Item не прошел модерацию или продавец/поставщик отказал по какой-то [уважительной] причине.
    *
    * @param itemId id итема, от которого отказывается продавец.
    * @param reasonOpt причина отказа, если есть.
    * @return
    */
  def refuseItem(itemId: Gid_t, reasonOpt: Option[String]): DBIOAction[RefuseItemResult, NoStream, RWT] = {
    lazy val logPrefix = s"refuseItemAction($itemId):"
    val dbAction = for {
      // Получить и заблокировать текущий item.
      mitem0 <- _prepareAwaitingItem(itemId)

      // Получить и заблокировать баланс юзера
      balance0 <- _item2balance(mitem0)

      // Разблокировать на балансе сумму с этого item'а
      amount2 <- mBalances.incrAmountAndBlockedBy(balance0, mitem0.price.amount)

      // Отметить item как отказанный в размещении
      mitem2 <- {
        LOGGER.debug(s"$logPrefix Unlocked user balance[${balance0.id.orNull}] amount ${mitem0.price.amount}: ${balance0.price.amount} => $amount2 ${balance0.price.currency}")

        // Чтобы вернуть новый item, не считывая его из таблицы повторно, имитируем его прямо тут...
        val mi2 = mitem0.copy(
          status      = MItemStatuses.Refused,
          dateStatus  = OffsetDateTime.now(),
          reasonOpt   = reasonOpt
        )
        mItems.query
          .filter(_.id === itemId)
          .map { i =>
            (i.status, i.dateStatus, i.reasonOpt)
          }
          .update((mi2.status, mi2.dateStatus, mi2.reasonOpt))
          .filter(_ == 1)
          .map(_ => mi2)
      }

      // Провести транзакцию по движению на счете
      mtxn2 <- {
        LOGGER.trace(s"$logPrefix Refused item, status ${mitem0.status} => ${mitem2.status}, reason = $reasonOpt")
        val mtxn0 = MTxn(
          balanceId  = balance0.id.get,
          amount     = mitem0.price.amount,
          txType     = MTxnTypes.Rollback,
          itemId     = Some(itemId)
        )
        mTxns.insertOne(mtxn0)
      }

    } yield {
      LOGGER.trace(s"$logPrefix Saved MTxn[${mtxn2.id.orNull}]")
      // Собрать итог работы экшена...
      RefuseItemResult(mitem2, mtxn2)
    }

    // Важно исполнять это транзакцией.
    dbAction.transactionally
  }


  /**
    * Простая и банальная финализация item'ов по id без какой-либо дополнительной логики.
    * Используется, когда нужно тупо завершить какой-то item в нормальном режиме.
    *
    * @param itemIds id завершаемых item'ов.
    * @param reasonOpt Причина завершения [None].
    * @param now Дата закрытия [now()].
    * @return Экшен, возвращающий кол-во обновлённых рядов.
    */
  def justFinalizeItems(itemIds   : Traversable[Gid_t],
                        reasonOpt : Option[String] = None,
                        now       : OffsetDateTime = OffsetDateTime.now
                       ): DBIOAction[Int, NoStream, Effect.Write] = {
    if (itemIds.isEmpty)
      LOGGER.error(s"justFinalizeItems($itemIds, $reasonOpt, $now): itemIds arg. must be non-empty, or this action makes no sence.")

    mItems.query
      .filter { i =>
        i.withIds1(itemIds) &&
          // Не надо повторно закрывать уже закрытые item'ы, это только даты сбивает.
          (!i.withStatuses( MItemStatuses.advDone ))
      }
      .map { i =>
        (i.status, i.dateEndOpt, i.dateStatus, i.reasonOpt)
      }
      .update((MItemStatuses.Finished, Some(now), now, reasonOpt))
  }

  /** Аналог justFinalizeItems(), но выборка item'ов не по id.
    *
    * @param nodeId id целевого узла/карточки.
    * @param iTypes Типы затрагиваемых item'ов (размещений).
    * @param statuses Затрагиваемые Item-статусы. Если не задано, то все незавершённые.
    * @param reasonOpt Опциональная причина завершения.
    * @param now Текущее время.
    * @return write-db-экшен, возвращающий кол-во затронутых рядов.
    */
  def justFinalizeItemsLike(nodeId    : String,
                            iTypes    : TraversableOnce[MItemType],
                            statuses  : TraversableOnce[MItemStatus]  = Nil,
                            rcvrIds   : Traversable[String]           = Nil,
                            reasonOpt : Option[String]                = None,
                            now       : OffsetDateTime                = OffsetDateTime.now
                           ): DBIOAction[Int, NoStream, Effect.Write] = {
    mItems.query
      .filter { i =>
        val i0 = {
          i.withNodeId(nodeId) && i.withTypes(iTypes) && {
            if (statuses.isEmpty)
              !i.withStatuses( MItemStatuses.advDone )
            else
              i.withStatuses( statuses )
          }
        }
        if (rcvrIds.isEmpty) {
          i0.?
        } else {
          i0 && i.withRcvrs(rcvrIds)
        }
      }
      .map { i =>
        (i.status, i.dateEndOpt, i.dateStatus, i.reasonOpt)
      }
      .update( (MItemStatuses.Finished, Some(now), now, reasonOpt) )
  }


  /**
    * Максимально быстрый поиск id контракта для указанного item'а.
    * @param itemId id итема.
    * @return DB-экшен с опциональным id контракта.
    */
  def getItemContractId(itemId: Gid_t): DBIOAction[Option[Gid_t], NoStream, Effect.Read] = {
    // Для ускорения организуем вложенный запрос:
    // http://slick.typesafe.com/doc/3.1.0/sql-to-slick.html#subquery
    // Найти orderId указанного item'а.
    val itemOrderIdsQ = mItems.query
      .filter(_.id === itemId)
      .map(_.orderId)

    // Найти contractId указанного ордера.
    mOrders.query
      .filter(_.id in itemOrderIdsQ)
      .map(_.contractId)
      .result
      .headOption
  }


  /**
    * У админов есть бесплатное размещение.
    * Код обработки бесплатного размещения вынесен сюда.
    */
  def maybeFreePricing(forceFree: Boolean)(f: => Future[MGetPriceResp]): Future[MGetPriceResp] = {
    if (forceFree)
      zeroPricingFut
    else
      f
  }


  /** Посчитать стоимость одного ордера.
    * Используется при подведении итогов заказа перед уходом на оплату в платежную систему.
    *
    * @param orderId id заказа. Обычно это заказ-корзина.
    * @return db-экшен со списком цен по валютам на выходе.
    */
  def getOrderPrices(orderId: Gid_t): DBIOAction[Seq[MPrice], NoStream, Effect.Read] = {
    for {
      pairs <- {
        mItems.query
          .filter { i =>
            i.orderId === orderId
          }
          .map { i =>
            (i.currencyCode, i.amount)
          }
          .groupBy(_._1)
          .map { case (currencyCode, grp) =>
            (currencyCode, grp.map(_._2).sum)
          }
          .result
      }
    } yield {
      val pricesIter = for {
        (currencyCode, amountOpt) <- pairs.iterator
        amount                    <- amountOpt
      } yield {
        // Пусть будет ошибка, если валюта неизвестна.
        val currency = MCurrencies.withName(currencyCode)
        MPrice(amount, currency)
      }
      pricesIter.toSeq
    }
  }
  def getOrderPricesFut(orderId: Gid_t): Future[Seq[MPrice]] = {
    slick.db.run {
      getOrderPrices(orderId)
    }
  }


  /**
    * Сборка ценников для списка ордеров.
    *
    * @param orderIds Список ордеров, которые надо обсчитать.
    * @return Карта ценников по ордерам.
    */
  def getOrdersPrices(orderIds: Traversable[Gid_t]): DBIOAction[Map[Gid_t, Seq[MPrice]], NoStream, Effect.Read] = {
    for {
      prices <- {
        mItems.query
          .filter { i =>
            i.orderId inSet orderIds
          }
          .groupBy { i =>
            (i.orderId, i.currencyCode)
          }
          .map { case ((orderId, currencyCode), grp) =>
            (orderId, currencyCode, grp.map(_.amount).sum)
          }
          .result
      }
    } yield {
      // Причесать полученные цены заказов в карту по orderId:
      prices
        .iterator
        .flatMap { case (orderId, currencyCode, amountOpt) =>
          for {
            amount    <- amountOpt
          } yield {
            val price = MPrice(
              amount    = amount,
              currency  = MCurrencies.withName(currencyCode)
            )
            orderId -> price
          }
        }
        .toSeq
        .groupBy(_._1)
        // Выкинуть orderId из полученных значений.
        .mapValues { orderPrices =>
          for ( (_, p) <- orderPrices ) yield p
        }
    }
  }


  /** Вычесть из цен остатки по балансам.
    *
    * @param prices Цены БЕЗ повторяющихся валют, иначе будет AssertionError.
    *               См. getOrderPrices() для получения подходящего списка цен.
    * @param balsMap Карта балансов по валютам.
    *                См. request.user.mBalancesFut и MBalances.balances2curMap() для получения подходящей карты остатоков по валютам.
    * @return Ценники: сколько надо доплатить за вычетом остатков по балансам.
    */
  def pricesMinusBalances(prices: Seq[MPrice], balsMap: Map[MCurrency, MBalance]): Seq[MPrice] = {
    assert(
      prices.iterator.map(_.currency).toSet.size == prices.size,
      s"Prices contains duplicates by currency: $prices"
    )

    prices
      .flatMap { op =>
        // Поискать в карте балансов остатки по текущей валюте.
        balsMap
          .get(op.currency)
          .filter(_.price.amount > 0)
          .fold[Option[MPrice]] (Some(op)) { curBal =>
            val balAmountAvail = curBal.price.amount
            // Есть какие-то деньги на валютном балансе юзера. Посчитать остаток.
            val needAmount = op.amount - balAmountAvail
            if (needAmount > 0) {
              val op2 = op.plusAmount( -balAmountAvail )
              Some(op2)
            } else {
              None
            }
          }
      }
  }


  /**
    * Вычислить стоимости оплаты заказа в валютах заказа.
    * Тут скорее дедубликация рутинного кода, нежели какие реальные действия.
    *
    * @param orderPricesFut Фьючерс с выхлопом getOrderPricesFut() или похожей функции.
    * @param mBalsFut Фьючерс с балансами из request.user.mBalancesFut
    * @return Фьючерс с ценниками по валютам на оплату.
    */
  def getPayPrices(orderPricesFut: Future[Seq[MPrice]], mBalsFut: Future[Seq[MBalance]]): Future[Seq[MPrice]] = {
    // Узнать, сколько остатков денег у юзера на балансах. Сгруппировать по валютам.
    val balsMapFut = for (ubs <- mBalsFut) yield {
      MCurrencies.hardMapByCurrency(ubs)
    }
    // Посчитать, сколько юзеру надо доплатить за вычетом остатков по балансам.
    for {
      orderPrices   <- orderPricesFut
      ubsMap        <- balsMapFut
    } yield {
      pricesMinusBalances(orderPrices, ubsMap)
    }
  }


  /**
    * Найти гарантированно открытый для оплаты ордер, принадлежащий к указанному контракту.
    * @param orderId id возможного ордера.
    * @param validContractId Гарантированно корректный id контракта.
    * @return Ордер.
    *         NSEE, если ордер не найден или не является корректным.
    */
  def getOpenedOrderForUpdate(orderId: Gid_t, validContractId: Gid_t): DBIOAction[MOrder, NoStream, Effect.Read] = {
    for {
      // Прочитать запрошенный ордер, одновременно проверяя необходимые поля.
      mOrderOpt <- {
        mOrders.query
          .filter { o =>
            (o.id === orderId) &&
              (o.contractId === validContractId) &&
              (o.statusStr inSet MOrderStatuses.canGoToPaySys.map(_.strId).toTraversable)
          }
          .result
          .headOption
          .forUpdate
      }

      // Проверить, что ордер существет, что относится к контракту юзера и доступен для оплаты.
    } yield {
      mOrderOpt.get
    }
  }

  /** Проверить ордер для платежной системы.
    * Работы идут в виде транзакции, которая завершается исключением при любой проблеме.
    *
    * Метод дёргается на check-стадии оплаты: платежная система ещё не списала деньги,
    * но хочет проверить достоверность присланных юзером данных по оплачиваемому заказу.
    * S.io проверяет ордер и "холдит" его, чтобы защититься от изменений до окончания оплаты.
    *
    * @param orderId Заявленный платежной системой order_id. Ордер будет проверен на связь с контрактом.
    * @param validContractId Действительный contract_id юзера, выверенный по максимуму.
    * @param claimedOrderPrices Цены заказа по мнению платежной системы.
    *                           Map: валюта -> цена.
    * @return DB-экшен.
    */
  def checkPayableOrder(orderId: Gid_t, validContractId: Gid_t, paySys: MPaySystem,
                        claimedOrderPrices: Map[MCurrency, IPrice]): DBIOAction[MOrder, NoStream, RWT] = {
    lazy val logPrefix = s"checkHoldOrder(o=$orderId,c=$validContractId):"

    val a = for {

      // Прочитать и проверить запрошенный ордер.
      mOrder <- getOpenedOrderForUpdate(orderId, validContractId = validContractId)

      // Прочитать балансы юзера по контракту, завернув их в мапу по валютам.
      uBals <- mBalances.findByContractId( validContractId )
      uBalsMap = MCurrencies.hardMapByCurrency( uBals )

      // Посчитать стоимость item'ов в ордере.
      orderPrices <- getOrderPrices(orderId)

      // Вычислить итоговые стоимости заказа в валютах.
      payPrices = pricesMinusBalances(orderPrices, uBalsMap)

      // Убедится, что всё ок с ценами, что они совпадают с заявленными платежной системой.
      if {
        LOGGER.trace(s"$logPrefix order=$mOrder\n uBalsMap=$uBalsMap\n orderPrices=$orderPrices\n payPrices=$payPrices")
        payPrices.forall { payPrice0 =>
          // Опционально поправить стоимость по минимальной стоимости.
          val payPrice = minPayPrice(paySys, payPrice0)
            .getOrElse(payPrice0)

          val claimedPrice = claimedOrderPrices( payPrice.currency )
          LOGGER.trace(s"$logPrefix payPrice=[$payPrice0 => $payPrice], claimedPrice = $claimedPrice")

          // Double сравнивать -- дело неблагодатное. Поэтому сравниваем в рамках погрешности:
          // 0.01 рубля (т.е. одна копейка) -- это предел, после которого цены не совпадают.
          val maxDiff = payPrice.currency.minAmount
          val matches = Math.abs(payPrice.amount - claimedPrice.amount) < maxDiff
          if (!matches)
            LOGGER.error(s"$logPrefix Claimed price = ${claimedPrice.amount} ${claimedPrice.currency}\n ReCalculated price = $payPrice\n They are NOT match.")
          matches
        }
      }

    } yield {
      // Вернуть что-нибудь...
      mOrder
    }

    // Транзакция обязательна, т.к. тут вопрос в безопасности: иначе возможно внезапное изменение ордера в рамках race-conditions.
    a.transactionally
  }


  /** Захолдить ордер, если ещё не захолжен.
    *
    * @param mOrder Исходный ордер.
    * @return DB-экшен, возвращающий обновлённый ордер.
    */
  def holdOrder(mOrder: MOrder): DBIOAction[MOrder, NoStream, Effect.Write] = {
    val holdStatus = MOrderStatuses.Hold
    def logPrefix = s"holdOrder(${mOrder.id.orNull}):"
    if (mOrder.status == holdStatus) {
      LOGGER.debug(s"$logPrefix Not holding order, because already hold since ${mOrder.dateStatus}: $mOrder")
      DBIO.successful(mOrder)
    } else {
      // Ордер не является замороженным. Заменяем ему статус.
      val o2 = mOrder.withStatus( holdStatus )
      for {
        rowsUpdated <- mOrders.saveStatus( o2 )
        if rowsUpdated == 1
      } yield {
        LOGGER.debug(s"$logPrefix Order marked as HOLD.")
        o2
      }
    }
  }

  /**
    * Разхолдить ордер назад в корзину.
    * @param orderId id ордера, подлежащего разморозке назад в корзину.
    * @return DB-экшен, возвращающий инстанс размороженного ордера.
    */
  def unholdOrder(orderId: Gid_t): DBIOAction[MOrder, NoStream, RWT] = {
    lazy val logPrefix = s"unHoldOrder($orderId)[${System.currentTimeMillis()}]:"

    val a = for {
      // Прочитать новый ордер в рамках транзакции.
      morderOpt <- mOrders.getById(orderId).forUpdate
      morder = morderOpt.get

      // Выполнять какие-то действия, только если позволяет текущий статус ордера.
      morder2 <- {
        if (morder.status == MOrderStatuses.Hold) {
          val morder22 = morder.withStatus( MOrderStatuses.Draft )
          for {
            // Поискать текущую корзину, вдруг юзер ещё что-то в корзину швырнул, пока текущий ордер был HOLD.
            cartOrderOpt    <- getCartOrder(morder.contractId)
              .forUpdate

            ordersUpdated <- mOrders.saveStatus( morder22 )
            if ordersUpdated == 1

            // Текущий ордер теперь снова стал корзиной.
            // Но возможно, что уже существует ещё одна корзина? Их надо объеденить в пользу текущего ордера.
            _ <- cartOrderOpt.fold [DBIOAction[Int, NoStream, WT]] {
              // Нет внезапной корзины, всё ок.
              LOGGER.trace(s"$logPrefix All ok, no duplicating cart-orders found.")
              DBIO.successful(0)
            } { suddenCartOrder =>
              // Обнаружена внезапная корзина. Переместить все item'ы из неё в текущий ордер, и удалить внезапную корзину.
              val suddenCartOrderId = suddenCartOrder.id.get
              LOGGER.info(s"$logPrefix Will merge cart orders $suddenCartOrderId => ${morder.id.orNull}, because new cart already exists.")
              mergeOrders(suddenCartOrderId, toOrderId = orderId)
            }
          } yield {
            // Вернуть обновлённый инстанс.
            morder22
          }

        } else if (morder.status == MOrderStatuses.Draft) {
          LOGGER.debug(s"$logPrefix Already draft since ${morder.dateStatus}. Nothing to do, skipped.")
          DBIO.successful( morder )
        } else {
          val msg = s"$logPrefix Order status is ${morder.status}, so cannot unhold. Only holded orders or drafts are allowed."
          LOGGER.error(msg)
          DBIO.failed( new IllegalArgumentException(msg) )
        }
      }

    } yield {
      LOGGER.debug(s"$logPrefix Order now draft. Was HOLD.")
      morder2
    }
    // Только атомарно, иначе совем опасно.
    a.transactionally
  }


  /** Объединение двух ордеров в пользу последнего из двух.
    *
    * @param orderIdForDelete id ордера, который подлежит уничтожению с переносом всех зависимых в новый ордер.
    * @param toOrderId Целевой ордер, в который будет происходить перенос.
    * @return DB-экшен, возвращающий кол-во удалённых ордеров. Т.е. или 1, или NSEE.
    */
  def mergeOrders(orderIdForDelete: Gid_t, toOrderId: Gid_t): DBIOAction[Int, NoStream, WT] = {
    lazy val logPrefix = s"mergeOrders($orderIdForDelete => $toOrderId)[${System.currentTimeMillis()}]:"
    val a = for {

      // Обновить все item'ы, транзакции и прочее.
      depRowsUpdated <- moveOrderDeps(orderIdForDelete, toOrderId = toOrderId)

      // Теперь удалить старую корзину. Список транзакций не проверяем, т.к. это же DRAFT-ордер, транзакций у таких не бывает.
      ordersDeleted <- {
        LOGGER.info(s"$logPrefix $depRowsUpdated order deps moved from oldOrder#$orderIdForDelete to target order#$toOrderId. Deleting old order...")
        mOrders.deleteById( orderIdForDelete )
      }
      if ordersDeleted == 1

      // Удалить любые дебажные вещи, касающиеся удаляемого ордера
      orderDebugsDeleted <- mDebugs.deleteByObjectId( orderIdForDelete )

    } yield {
      LOGGER.info(s"$logPrefix Deleted $ordersDeleted cart-order with $orderDebugsDeleted order's debugs.")
      ordersDeleted
    }
    a.transactionally
  }


  /** Промигрировать все зависимости одного ордера в пользу другого ордера.
    *
    * @param fromOrderId id старого ордера.
    * @param toOrderId id нового ордера.
    * @return db-экшен, возвращающий кол-во обновлённых рядов в целом.
    */
  def moveOrderDeps(fromOrderId: Gid_t, toOrderId: Gid_t): DBIOAction[Int, NoStream, Effect.Write] = {
    for {
      // Обновить все item'ы.
      itemsUpdated <- mItems.query
        .filter(_.orderId === fromOrderId)
        .map(_.orderId)
        .update(toOrderId)

      // Обновить все транзакции.
      txnsUpdated <- mTxns.query
        .filter(_.orderIdOpt === fromOrderId)
        .map(_.orderIdOpt)
        .update(Some(toOrderId))

    } yield {
      LOGGER.debug(s"moveOrderDeps($fromOrderId => $toOrderId): ")
      itemsUpdated + txnsUpdated
    }
  }


  /** Поиск и устранение повисших HOLD-ордеров в базе.
    * Можно вызывать по cron'у.
    *
    * @return Фьючерс.
    */
  def findReleaseStalledHoldOrders(): Future[_] = {
    val hours = RELEASE_HOLD_ORDERS_AFTER_HOURS
    val oldNow = OffsetDateTime.now.minusHours( hours )
    val stalledOrderIdsFut = slick.db.run {
      mOrders.query
        .filter { o =>
          (o.statusStr === MOrderStatuses.Hold.strId) && (o.dateStatus < oldNow)
        }
        .map(_.id)
        .take( MAX_STALLED_HOLDED_ORDERS_PER_ITERATION )
        .result
    }

    lazy val logPrefix = s"findReleaseStalledHoldOrders[${System.currentTimeMillis()}]:"

    for {
      orderIds <- stalledOrderIdsFut
      orderIdsCount = orderIds.size
      // Чтобы не нагружать сильно систему, обходим ордеры последовательно.
      errorsCount <- {
        if (orderIdsCount > 0)
          LOGGER.trace(s"$logPrefix Found $orderIdsCount orders: ${orderIds.mkString(", ")}")
        orderIds.foldLeft( Future.successful(0) ) { (accFut, orderId) =>
          accFut
            .flatMap { errCounter =>
              slick.db
                .run {
                  unholdOrder(orderId)
                }
                .map { _ => errCounter }
                .recover { case ex: Throwable =>
                  LOGGER.error(s"$logPrefix failed to unhold order $orderId", ex)
                  errCounter + 1
                }
            }
        }
      }
    } yield {
      // Логгировать завершение, не сильно мусоря в логах.
      if (orderIdsCount > 0) {
        val msg = s"$logPrefix ${hours}h($oldNow) Finished now with $orderIdsCount orders, $errorsCount failures."
        if (errorsCount > 0) {
          LOGGER.error(msg)
        } else {
          LOGGER.info(msg)
        }
      }
    }
  }


  /** Штатное зачисление денег на баланс какого-то юзера через внешнюю платежную систему.
    *
    * @param contractId id контракта юзера.
    * @param mprice Объём средств.
    * @param psTxnUid Уникальный id инвойса или что-то в этом роде на стороне платежной системы.
    * @param orderIdOpt id ордера, если платёж связан с заказом.
    * @param comment Коммент к платежу. Например, название платёжной системы.
    * @return MTxn.
    */
  def incrUserBalanceFromPaySys(contractId: Gid_t, mprice: MPrice, psTxnUid: String, orderIdOpt: Option[Gid_t] = None,
                                comment: Option[String] = None): DBIOAction[MTxn, NoStream, RWT] = {
    // Найти баланс юзера для текущей валюты.
    val a = for {
      // Узнать текущий баланс юзера
      usrBalance0Opt <- mBalances.getByContractCurrency(contractId, mprice.currency)
        .forUpdate

      // Инициализировать/обновить баланс.
      usrBalance2 <- {
        usrBalance0Opt.fold[DBIOAction[MBalance, NoStream, Effect.Write]] {
          val b = MBalance(
            contractId = contractId,
            price = mprice
          )
          mBalances.insertOne(b)

        } { usrBalance0 =>
          mBalances.incrAmountBy(usrBalance0, mprice.amount)
            .map(_.get)   // Этот баланс уже существует и передан в параметре, Option можно смело игнорировать.
        }
      }

      // Записать в БД транзакцию зачисления денег на баланс юзера.
      balIncrTxn <- {
        val txn0 = MTxn(
          balanceId       = usrBalance2.id.get,
          amount          = mprice.amount,
          txType          = MTxnTypes.PaySysTxn,
          orderIdOpt      = orderIdOpt,
          paymentComment  = comment,
          psTxnUidOpt     = Some(psTxnUid),
          datePaid        = Some( OffsetDateTime.now() )
        )
        mTxns.insertOne(txn0)
      }

    } yield {
      LOGGER.debug(s"incrUserBalanceFromPaySys($contractId, $mprice, $psTxnUid, $orderIdOpt): done, comment=$comment")
      balIncrTxn
    }
    // Нужна транзакция, т.к. очень денежные тут дела.
    a.transactionally
  }


  /** Найти в базе платежную транзакцию, относящуюся к указанному заказу.
    * Т.е. транзакцию зачисления денег из платежной системы в sio-биллинг.
    *
    * @param orderId Номер заказа.
    * @return DB-экшен с опциональной транзакцией.
    */
  def getOrderTxns(orderId: Gid_t): DBIOAction[Seq[MTxn], Streaming[MTxn], Effect.Read] = {
    mTxns.query
      .filter { t =>
        (t.orderIdOpt === orderId) &&
          (t.txTypeStr === MTxnTypes.PaySysTxn.strId)
      }
      // Если вдруг больше одной транзакции, то интересует самая ранняя. Такое возможно из-за логических ошибок при аппруве item'ов.
      .sortBy(_.id.asc)
      .result
  }


  /** Найти N последних заказов.
    *
    * @param contractId id контракта.
    * @param limit Лимит.
    * @param offset Оффсет.
    * @return DB-экшен, возвращающий ордеры, новые сверху.
    */
  def findLastOrders(contractId: Gid_t, limit: Int, offset: Int = 0): DBIOAction[Seq[MOrder], Streaming[MOrder], Effect.Read] = {
    mOrders.query
      .filter { o =>
        o.contractId === contractId
      }
      .sortBy(_.id.desc)
      .drop( offset )
      .take( limit )
      .result
  }


  /** Собрать минимальную и достаточную геоинфу для рендера разноцветных кружочков на карте размещений.
    *
    * @param query Исходный запрос item'ов. Например, выхлоп от AdvGeoBillUtil.findCurrentForAdQ().
    *
    * @return Пачка из Option'ов, т.к. все затрагиваемые столбцы базы заявлены как NULLable,
    *         и slick не может это проигнорить:
    *         (geo_shape, id, isAwaitingMdr).
    */
  def onlyGeoShapesInfo(query: Query[MItems#MItemsTable, MItem, Seq], limit: Int = 500): DBIOAction[Seq[AdvGeoShapeInfo_t], Streaming[AdvGeoShapeInfo_t], Effect.Read] = {
    query
      // WHERE не пустой geo_shape
      .filter(_.geoShapeStrOpt.isDefined)
      // GROUP BY geo_shape
      .groupBy(_.geoShapeStrOpt)
      .map { case (geoShapeStrOpt, group) =>
        // Делаем правильный кортеж: ключ -- строка шейпа, id - любой, status -- только максимальный
        (geoShapeStrOpt,
          group.map(_.id).max,
          group.map(_.statusStr).max =!= MItemStatuses.AwaitingMdr.strId
        )
      }
      // LIMIT 200
      .take(limit)
      .result
    // TODO Нужно завернуть кортежи в MAdvGeoShapeInfo. .map() не котируем, т.к. ломает streaming.
  }

}


/** Интерфейс для DI. */
trait IBill2UtilDi {
  /** Инстанс DI-поля. */
  def bill2Util: Bill2Util
}
