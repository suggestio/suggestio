package util.billing

import akka.stream.Materializer

import java.time.{Duration, OffsetDateTime}
import java.util.concurrent.atomic.AtomicInteger
import akka.stream.scaladsl.{Keep, Sink}

import javax.inject.Inject
import io.suggest.bill._
import io.suggest.bill.cart.{MCartIdeas, MCartResolution}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.dbg.MDebugs
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.mbill2.m.item.{IMItem, MItem, MItems}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.txn.{MTxn, MTxnMetaData, MTxnType, MTxnTypes, MTxns}
import io.suggest.mbill2.util.effect._
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.pay.MPaySystem
import io.suggest.primo.id._
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import models.adv.geo.cur.AdvGeoShapeInfo_t
import slick.sql.SqlAction
import io.suggest.enum2.EnumeratumUtil.ValueEnumEntriesOps
import io.suggest.es.model.EsModel
import io.suggest.model.SlickHolder
import io.suggest.n2.bill.MNodeBilling
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.streams.StreamsUtil
import io.suggest.text.StringUtil
import io.suggest.util.JmxBase
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

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
final class Bill2Util @Inject() (
                                  injector: Injector,
                                )
  extends MacroLogsImpl
{

  private lazy val configuration = injector.instanceOf[Configuration]
  private lazy val esModel = injector.instanceOf[EsModel]
  lazy val bill2Conf = injector.instanceOf[Bill2Conf]
  lazy val mOrders = injector.instanceOf[MOrders]
  lazy val mContracts = injector.instanceOf[MContracts]
  lazy val mItems = injector.instanceOf[MItems]
  lazy val mBalances = injector.instanceOf[MBalances]
  lazy val mTxns = injector.instanceOf[MTxns]
  protected lazy val mNodes = injector.instanceOf[MNodes]
  lazy val mDebugs = injector.instanceOf[MDebugs]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]
  private lazy val tfDailyUtil = injector.instanceOf[TfDailyUtil]
  lazy val slickHolder = injector.instanceOf[SlickHolder]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]
  implicit private lazy val mat = injector.instanceOf[Materializer]

  import Bill2Util._
  import slickHolder.slick
  import slick.profile.api._
  import streamsUtil.Implicits._
  import esModel.api._


  /** Через сколько времени считать ордер повисшим и разворачивать его назад в ордер-корзину? */
  private lazy val RELEASE_HOLD_ORDERS_AFTER_HOURS: Int = {
    configuration.getOptional[Int]("billing.orders.release.hold.after.hours")
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
    if (iType ==* MItemTypes.BalanceCredit) {
      // Это пополнение собственного баланса. Проходит без промежуточных шагов и сразу же закрывается.
      MItemStatuses.Finished

    } else if (iType.sendToMdrOnOrderClose) {
      // Услуга, отправляемая на модерацию.
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

  def cbcaNodeOptFut = mNodes.getByIdCache( bill2Conf.CBCA_NODE_ID )


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
        val res = (ncIdOpt ==* mc.id)
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
      for (opt <- fut if opt.isEmpty) {
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
      slick.db.run {
        initNodeContract(mnode)
      }
    }
  }


  def initNodeContract(mnode: MNode) = {
    lazy val logPrefix = s"initNodeContract(${mnode.idOrNull}):"

    (for {
      // Создать новый контракт в БД биллинга
      mc2 <- {
        val mc = MContract()
        mContracts.insertOne(mc)
      }
      if mc2.id.nonEmpty

      // Сохранить id свежесозданного контракта в текущую ноду
      mnode2 <- DBIO.from {
        LOGGER.trace(s"$logPrefix Init contract#${mc2.id.orNull}")

        mNodes.tryUpdate(mnode) {
          MNode.billing
            .andThen( MNodeBilling.contractId )
            .replace( mc2.id )
        }
      }
    } yield {
      LOGGER.debug(s"$logPrefix Done, contract#${mc2.id.orNull}")
      EnsuredNodeContract(mc2, mnode2)
    })
      .transactionally
  }


  /** Поиск ордера-корзины. */
  def getCartOrder(contractId: Gid_t) = getLastOrder(contractId, MOrderStatuses.Draft)

  /** Поиск id ордера-корзины. */
  def getCartOrderId(contractId: Gid_t): DBIOAction[Option[Gid_t], NoStream, Effect.Read] = {
    _getLastOrderSql( contractId, MOrderStatuses.Draft, MOrderStatuses.Hold )
      .map(_.id)
      .result
      .headOption
  }

  def orderHasItems(orderId: Gid_t): DBIOAction[Boolean, NoStream, Effect.Read] = {
    mItems
      .query
      .filter { i =>
        i.orderId === orderId
      }
      .exists
      .result
  }

  private def _getLastOrderSql(contractId: Gid_t, statuses: MOrderStatus*) = {
    mOrders.query
      .filter { q =>
        val query0 = (q.contractId === contractId)
        // Append order-status filtering, if statuses non empty:
        if (statuses.isEmpty) query0
        else query0 && (q.statusStr inSet statuses.map(_.value))
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
    (for {
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
    })
      .transactionally
  }


  def deleteItem(itemId: Gid_t): DBIOAction[Int, NoStream, WT] = {
    (for {
      itemsDeleted  <- mItems.deleteById(itemId)
      debugsDeleted <- mDebugs.deleteByObjectId( itemId )
    } yield {
      LOGGER.debug(s"deleteItem($itemId): Deleted $itemsDeleted items with $debugsDeleted debugs.")
      itemsDeleted
    })
      .transactionally
  }


  def deleteContract(contractId: Gid_t): DBIOAction[Int, NoStream, WT] = {
    (for {
      contractsDeleted <- mContracts.deleteById( contractId )
      debugsDeleted    <- mDebugs.deleteByObjectId( contractId )
    } yield {
      LOGGER.info(s"deleteContract($contractId): Deleted $contractsDeleted contracts, $debugsDeleted debugs")
      contractsDeleted
    })
      .transactionally
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


  def findDraftItems(orderId: Gid_t): DBIOAction[Seq[MItem], Streaming[MItem], Effect.Read] = {
    mItems.findByOrderIdBuilder(orderId)
      .filter(_.statusStr === MItemStatuses.Draft.value)
      .result
      .forUpdate
  }
  def orderWithDraftItems(order: MOrder): DBIOAction[MOrderWithItems, NoStream, Effect.Read] = {
    val orderId = order.id.get
    for {
      mitems        <- findDraftItems( orderId )
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
      res           <- orderWithDraftItems(order)
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
  def itemsHasPrice(mitems: Iterable[MItem]): Boolean = {
    mitems
      .exists { item =>
        item.price.amount > 0
      }
  }


  /** slick-экшен для запуска внутри транзакции для исполнения цельного ордера.
    * Считается, что все оплаты уже посчитаны, проверены и списаны.
    * Т.е. это жесткое закрытие ордера, и не всегда уместно.
    *
    * @param orderWithItems Обрабатываемый ордер вместе с item'ами.
    * @return DBIO-экшен, с какими-то данными или без них.
    */
  def closeOrder(orderWithItems: MOrderWithItems): DBIOAction[MOrderWithItems, NoStream, Effect.Write] = {
    for {
      // Обновляем статус ордера.
      morder3 <- {
        val morder2 = (MOrder.status replace MOrderStatuses.Closed)( orderWithItems.order )
        for {
          ordersUpdated <- mOrders.saveStatus( morder2 )
          if ordersUpdated ==* 1
        } yield {
          morder2
        }
      }

      // Обновляем item'ы ордера, возвращаем обновлённые инстансы.
      mitems3 <- {
        val actions = for (itm <- orderWithItems.items) yield {
          val itm2 = itm.withStatus( orderClosedItemStatus(itm.iType) )
          for {
            itemsUpdated <- mItems.updateStatus(itm2)
            if itemsUpdated ==* 1
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
  def maybeExecuteOrder(order: MOrderWithItems): DBIOAction[MCartResolution, NoStream, RW] = {

    lazy val logPrefix = s"maybeExecuteCart(${order.order.id.orNull} ${System.currentTimeMillis}):"

    LOGGER.trace(s"$logPrefix There are ${order.items.size} items: ${order.items.iterator.flatMap(_.id).mkString(", ")}")

    if (order.items.isEmpty) {
      // Корзина пуста. Should not happen.
      LOGGER.trace(s"$logPrefix no items in the cart")
      DBIO.successful( MCartResolution(MCartIdeas.NothingToDo) )

    } else if ( !itemsHasPrice(order.items) ) {
      // Итемы есть, но всё бесплатно. Исполнить весь этот контракт прямо тут.
      LOGGER.trace(s"$logPrefix Only FREE items in cart.")
      for {
        _ <- closeOrder( order )
      } yield {
        MCartResolution( MCartIdeas.OrderClosed )
      }

    } else {

      // Товары есть, и они не бесплатны.
      LOGGER.trace(s"$logPrefix There are priceful items in cart.")

      for {
        // Узнаём текущее финансовое состояние юзера...
        balances    <- mBalances.findByContractId( order.order.contractId ).forUpdate

        // Строим карту полученных балансов юзера для удобства работы:
        balancesMap = MCurrencies.hardMapByCurrency(balances)

        // Считаем полную стоимость заказа-корзины.
        totalPrices = MPrice.toSumPricesByCurrency(order.items).values

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
                        orderIdOpt  = order.order.id,
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
            val idea = MCartResolution(
              MCartIdeas.NeedMoney,
              //newCart = Some( order ),
              needMoney = Some( notEnoughtPrices ),
            )
            DBIO.successful(idea)

          } else {
            // Есть достаточно средств на балансах -- их и тратим.
            for {
              balances2 <- DBIO.sequence(balUpdActions)
              // Закрываем исходный ордер корзины.
              order2    <- closeOrder(order)
            } yield {
              MCartResolution(
                MCartIdeas.OrderClosed,
                newCart     = Some( order2 ),
                newBalances = Some( balances2 ),
              )
            }
          }
        }

      } yield {
        res
      }
    }
  }

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
  def forceFinalizeOrder(owi: MOrderWithItems): DBIOAction[ForceFinalizeOrderRes, NoStream, RWT] = {
    val orderId = owi.order.id.get
    val contractId = owi.order.contractId

    lazy val logPrefix = s"forceFinalizeOrder($orderId):"
    LOGGER.trace(s"$logPrefix Starting, items = ${owi.items.toIdIter[Gid_t].mkString(", ")}")

    (for {

      // Узнаём текущее финансовое состояние юзера в контексте текущего ордера...
      balsMap0 <- {
        // Собираем валюты, которые упомянуты в item'ах заказа.
        val itemCurrencies = MCurrencies
          .toCurrenciesIter {
            MPrice.toPricesIter(owi.items)
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
        val acc9 = owi.items
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
                if itemsUpdated ==* 1
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

        lazy val skippedItemIds = acc9.skipped
          .toIdIter[Gid_t]
          .to( Set )

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
                  if balsUpdated ==* 1
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
                    (t.itemIdOpt inSet skippedItemIds) &&
                    (t.orderIdOpt === orderId)
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

          orderDealTxnIdOpt <- orderDealTransactionId( owi.order.id.get )

          // Закрыть статус текущего ордера.
          morder9 <- {
            val nextOrderStatus = MOrderStatuses.afterPaymentStatus( orderDealTxnIdOpt.nonEmpty )
            val morder2 = (MOrder.status replace MOrderStatuses.Closed)( owi.order )
            for {
              ordersUpdated <- mOrders.saveStatus( morder2 )
              if ordersUpdated ==* 1
            } yield {
              LOGGER.debug(s"$logPrefix Order#$orderId status updated to $nextOrderStatus, paySysDeal txnId#${orderDealTxnIdOpt.orNull}.")
              morder2
            }
          }

        } yield {
          LOGGER.trace(s"$logPrefix Finished processing order#$orderId with ${owi.items.size} orig.items. SkippedCartOpt=${skippedCartOpt.flatMap(_.id).orNull} (normally, null).")
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
    })
      .transactionally
  }


  def orderDealTransactionQuery(orderId: Gid_t): Query[mTxns.MTxnsTable, MTxn, Seq] = {
    mTxns
      .query
      .filter { tx =>
        (tx.orderIdOpt === orderId) &&
        (tx.txTypeStr === MTxnTypes.Payment.value) &&
        (tx.psTxnUidOpt.nonEmpty) &&
        (tx.psDealIdOpt.nonEmpty) &&
        (tx.metadataJson.nonEmpty) &&
        (tx.datePaidOpt.nonEmpty)
      }
      .take( 1 )
  }

  /** Next order status after payment.
    * @return Deal-data transaction id#.
    */
  def orderDealTransactionId(orderId: Gid_t): DBIOAction[Option[Gid_t], NoStream, Effect.Read] = {
    for {
      txnIdOpt <- orderDealTransactionQuery( orderId )
        .map( _.id )
        .result
        .headOption
    } yield {
      LOGGER.trace(s"orderAfterPaymentStatus($orderId): Order have paysystem-deal transaction?${txnIdOpt.orNull}")
      txnIdOpt
    }
  }


  /** Логика заливки на баланс денег. Обычно используется для нужд валютных кошельков CBCA.
    *
    * @param contractId id контракта получателя денег (обычно CBCA).
    * @param price Общая цена пополнения в валюте.
    * @return DBIOAction создания/обновления кошелька узла.
    */
  def increaseBalanceAsIncome(contractId: Gid_t, price: MPrice): DBIOAction[MTxn, NoStream, RWT] = {
    lazy val logPrefix = s"increaseBalanceSimple(c#$contractId, $price)[${System.currentTimeMillis()}]:"
    (for {
      // Сразу искать баланс в нужной валюте:
      balOpt      <- mBalances.getByContractCurrency(contractId, price.currency).forUpdate

      // Залить средства на баланс целевого узла.
      bal2 <- balOpt.fold [DBIOAction[MBalance, NoStream, Effect.Write]] {
        val cb0 = MBalance(contractId, price)
        LOGGER.trace(s"$logPrefix Initializing new balance for this currency: $cb0")
        mBalances.insertOne(cb0)

      } { cb0 =>
        LOGGER.trace(s"$logPrefix Updating existing balance $cb0")
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
        LOGGER.trace(s"$logPrefix Inserting txn for balance #${bal2.id.orNull}")
        mTxns.insertOne(ctxn0)
      }

    } yield {
      LOGGER.trace(s"$logPrefix: Done")
      ctxn2
    })
      .transactionally
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
  private def _item2balance(mitem: MItem) = {
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
  def prepareMoneyReceiver(nodeId: String): Future[EnsuredNodeContract] = {
    lazy val logPrefix = s"prepareMoneyReceiver($nodeId)#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix Starting")

    // Организовать сборку данных по контракту получателя.
    for {

      // Гуляем по графу в поисках узла-получателя денег. Это узел с контрактом или узел-юзер.
      (mrCandidateNodes, nodesInfoMap) <- mNodes.walkCache(
        acc0 = List.empty[MNode] -> Map(nodeId -> Set.empty[Int]),
        ids  = Set.empty + nodeId,
      ) {
        case (acc0 @ (nodesAcc0, nodeIdsAcc0), mnode) =>
          if (
            mnode.billing.contractId.nonEmpty ||
            (mnode.common.ntype ==* MNodeTypes.Person)
          ) {
            // Это подходящий узел, запихнуть его в акк.
            LOGGER.trace(s"$logPrefix Found node#${mnode.idOrNull} contract#${mnode.billing.contractId.orNull}")
            val nodesAcc2 = mnode :: nodesAcc0
            val acc2 = (nodesAcc2, nodeIdsAcc0)
            (acc2, Set.empty)

          } else if (nodesAcc0.isEmpty) {
            // Этот узел не подходит и подходящих найденных кандидатов нет, поэтому вернуть ownerIds
            val nextOwnEdges = mnode.edges
              .withPredicateIter( MPredicates.OwnedBy )
              .toList

            val nextIds = nextOwnEdges
              .iterator
              .flatMap(_.nodeIds)
              // Этот узел не запрашивался ранее (защита от циклов в графе):
              .filterNot { nodeIdsAcc0.contains }
              .toSet

            // Залить в nodeIdsAcc0 данные по узлам и эджам. Надо сохранить sortId для скоринга кандидатов.
            val nodeIdsAcc2 = nextOwnEdges.foldLeft( nodeIdsAcc0 ) { (xnodeIdsAcc0, e) =>
              // TODO Opt можно отбрасывать низко-приоритетные эджи прямо тут. Сейчас инфа по всем приоритетам здесь только накапливается с минимальным анализом.
              val orderSet = e.order.toSet
              e.nodeIds.foldLeft(xnodeIdsAcc0) { (xxNodeIdsAcc0, nodeId) =>
                val v2 = xxNodeIdsAcc0
                  .get( nodeId )
                  .map(_ ++ orderSet)
                  .getOrElse( orderSet )
                xxNodeIdsAcc0.updated(nodeId, v2)
              }
            }

            LOGGER.trace(s"$logPrefix Looking for parent-nodes of #${mnode.idOrNull}: [${nextIds.size}] - ${nextIds.mkString(" ")}")
            val acc2 = (nodesAcc0, nodeIdsAcc2)
            (acc2, nextIds)

          } else {
            // Узел не подходит, и уже есть найденные подходящие узлы-кандидаты.
            // TODO Возможно, это искусственное подавление walk-цикла архитектурно-неверно: ведь на верхних уровнях могут быть более приоритетные деньго-получатели (на более сложных схемах деньго-проведения).
            LOGGER.trace(s"$logPrefix Skipping #${mnode.idOrNull}, nodes already found.")
            (acc0, Set.empty)
          }
      }

      // Надо разобраться, есть ли среди узлов-кандидатов подходящий узел-получатель для денег.
      // Поискать первый законтрактованный узел, или поискать юзера и инициализировать контракт.
      // Если нет подходящих получателей, то заюзать узел CBCA для получения денег.
      // Для сложных схем это может быть неудобно или не прокатить, но пусть будет так.

      rcvrNode <- {
        LOGGER.trace(s"$logPrefix Found ${mrCandidateNodes.length} candidate-nodes for money receivers.")
        if (mrCandidateNodes.isEmpty) {
          // Не найдено ни одного узла-кандидата, вероятно суперюзеры создали и разместили какой-то узел в выдаче.
          val cbcaNodeId = bill2Conf.CBCA_NODE_ID
          val r = mNodes.getByIdCache( cbcaNodeId )
            .map(_.get)
          LOGGER.warn(s"$logPrefix Not money-rcvrs, will drop money to CBCA#$cbcaNodeId")
          r
        } else {
          // Есть хотя бы один узел-кандидат. Выбрать наиболее подходящий узел из списка кандидатов.
          // Поискать сначала по собранным приоритетам узлов из эджей:
          val prioNodeOpt = {
            val iter = nodesInfoMap
              .iterator
              .filter( _._2.nonEmpty )
            // Если задан приоритет у хотя бы одного узла, значит он и будет получателем денег.
            // Чем ниже значение MEdge.order - тем выше приоритет.
            OptionUtil.maybe(iter.nonEmpty) {
              iter
                .map { case (k, vs) => (k, vs.min) }
                .minBy(_._2)
                // TODO А если несколько одинаковых минимальных значений получилось? Пока пусть будет рандом.
                ._1
            }
          }

          lazy val nodesMap = mrCandidateNodes
            .zipWithIdIter[String]
            .to( Map )

          val r = prioNodeOpt
            .flatMap( nodesMap.get )
            .orElse {
              // Ищем первый законтактованный узел. Если его нет, то берём первый попавшийся узел.
              // TODO Этот выбор носит скорее рандомный характер, нежеле осмысленный. Удалить эту ветвь orElse?
              if (mrCandidateNodes.lengthCompare(1) > 0)
                LOGGER.warn(s"$logPrefix Multiple owners for money-receive w/o MEdge.order prios: [${mrCandidateNodes.iterator.flatMap(_.id).toSet.mkString(" ")}]. Choosing first contracted...")
              mrCandidateNodes
                .find(_.billing.contractId.nonEmpty)
            }
            .getOrElse( mrCandidateNodes.head )

          LOGGER.trace(s"$logPrefix Money rcvr => ${r.idOrNull} contractId#${r.billing.contractId.orNull} prioNode#${prioNodeOpt.orNull}")
          Future.successful(r)
        }
      }

      enc         <- ensureNodeContract(rcvrNode)

    } yield {
      LOGGER.debug(s"$logPrefix => Money rcvr#${enc.mnode.idOrNull}, contract#${enc.contract.id.orNull}")
      enc
    }
  }


  def prepareAllMoneyReceivers(items: Iterable[MItem]): Future[Iterable[MoneyRcvrData]] = {
    for {
      allMoneyRcvrsRaw <- Future.traverse( items )( prepareMoneyReceivers )
    } yield {
      allMoneyRcvrsRaw.flatten
    }
  }


  /** Here method prepares steps for making a p2p+sio deal via payment system.
    *
    * @param paySystem Payment system.
    * @param isDealSupportedPaySys Is payment system supports deals now? Logically, this may be filtered outside here, but for code deduplication it lives here.
    * @param moneyRcvrs
    * @return Optional result with deal prices.
    */
  def prepareDealData(paySystem: MPaySystem, isDealSupportedPaySys: Boolean, moneyRcvrs: Iterable[MoneyRcvrData]): Option[ExtDealPrepareResult] = {
    lazy val logPrefix = s"prepareDealData($paySystem?$isDealSupportedPaySys, [${moneyRcvrs.size}]):"

    for {
      _ <- Option.when( isDealSupportedPaySys && moneyRcvrs.nonEmpty && bill2Conf.DEAL_PAYOUTS_ENABLED )(())

      // Find paysystems and deals:
      dealCanPayRes = canPayExtTxn( paySystem, moneyRcvrs )
      if {
        val r = dealCanPayRes.dealWith.nonEmpty
        if (r) LOGGER.trace(s"$logPrefix Found ${dealCanPayRes.dealWith.size} dealable money-rcvrs: [${dealCanPayRes.dealWith.iterator.flatMap(_.enc.mnode.id).mkString(",")}], and ${dealCanPayRes.outOfDeal.size} non-dealable.")
        else   LOGGER.debug(s"$logPrefix Not found any dealable money-rcvrs with payout. Will grab money for rest ${dealCanPayRes.outOfDeal.size} money-rcvrs.")
        r
      }

      // Now, let's move sio-comission from dealCanPayRes into comissionDeal prices.
      (dealSioComissions, dealSellersOnly) = dealCanPayRes.dealWith.partition { _.isSioComissionPrice }

      if {
        // Self-control: ensure non-emptyness of both deal-parts.
        val r = dealSioComissions.nonEmpty
        // If r.isEmpty,  here invalid situation. If continue, all deal money goes to sellers party.
        // So, suggest.io will have no profit. Also, Suggest.io will pay comission to external pay-system from suggest.io own money.
        if (!r) LOGGER.error( s"$logPrefix No s.io comission detected in dealWith list. Should never happen." )
        r
      } && {
        // No sellers after s.io-comission partitioning? => Nobody to deal money with and zero deal payout price.
        val r = dealSellersOnly.nonEmpty
        if (!r) LOGGER.warn(s"$logPrefix Only s.io comission deceted in dealWith, and no sellers. This is normal? Normally, this conditions should be filtered on upper levels of code.")
        r
      }

      // Untoched for opening deal moneyRcvr info, packed into original deal decision:
      dealRcvrsForPaySystem = {
        LOGGER.trace(s"$logPrefix Moving s.io comission from dealWith to outOfDeal:\n s.io comission = ${dealSioComissions.mkString(", ")}\n retained payouts = ${dealSellersOnly.mkString(", ")}")
        dealCanPayRes.copy(
          dealWith  = dealSellersOnly,
          outOfDeal = dealSioComissions ++ dealCanPayRes.outOfDeal,
        )
      }

    } yield {
      val dealData = ExtDealPrepareResult( dealRcvrsForPaySystem )
      LOGGER.trace(s"$logPrefix Done, deal is available: $dealData")
      dealData
    }
  }


  /** Распедалить по бенефициарам деньги в случае проведения указанного item'а.
    *
    * @param mitem Item, который планируется проводить.
    * @return Список данные по узлам-бенефециарам, куда пойдёт сколько денег.
    */
  def prepareMoneyReceivers(mitem: MItem): Future[List[MoneyRcvrData]] = {
    // Нужна Map[nodeId -> EnsuredNodeContract] в зависимости от itype, тарифа (комиссии s.io) и прочего.
    lazy val logPrefix = s"prepareMoneyReceivers(${mitem.id.orNull}):"
    if (mitem.price.amount <= 0) {
      LOGGER.trace(s"$logPrefix Zero price - no money receivers")
      Future.successful( Nil )

    } else {
      val items = mitem :: Nil

      mitem
        .rcvrIdOpt
        .filter { rcvrId =>
          val r = !mitem.iType.moneyRcvrOnlyCbca &&
                  (rcvrId !=* bill2Conf.CBCA_NODE_ID)
          LOGGER.trace(s"$logPrefix rcvrId#$rcvrId Money rcvr ONLY cbca?$r")
          r
        }
        .fold {
          // На item'ах данного типа все деньги должны сливаются в узел CBCA без учёта тарифа.
          val mrNodeId = bill2Conf.CBCA_NODE_ID
          LOGGER.trace(s"$logPrefix money rcrv = $mrNodeId == cbca")
          for {
            enc <- prepareMoneyReceiver(mrNodeId)
          } yield {
            MoneyRcvrData( mitem.price, enc, isSioComissionPrice = true, items ) :: Nil
          }
        } { rcvrId =>
          for {
            mnodeOpt <- mNodes.getByIdCache(rcvrId)
            mnode = mnodeOpt.get
            tfDaily <- tfDailyUtil.nodeTf( mnode )

            // На руках есть тариф. Надо понять, как распределять деньги между ресивером и CBCA.

            // Вычисляем итоговую комиссию s.io:
            comissionPc = tfDaily.comissionPc
              .getOrElse( tfDailyUtil.COMISSION_DFLT )

            // Является ли cbca получателем денег?
            (cbcaComissedPriceOpt0, rcvrPriceOpt0) = {
              LOGGER.trace(s"$logPrefix item price=${mitem.price} comission=$comissionPc")
              if (comissionPc >= tfDailyUtil.COMISSION_FULL) {
                // Все деньги улетают в CBCA
                (Some(mitem.price), None)
              } else if (comissionPc > 0d) {
                // Делим деньги между CBCA и узлом. Комиссию округляем вниз, и вычисляем премию узла вычитанием комисси.
                val comissionPrice = mitem.price * comissionPc
                val retainedPrice = mitem.price plusAmount -comissionPrice.amount
                LOGGER.trace(s"$logPrefix Split price: cbca=>$comissionPrice rcvr#$rcvrId=>$retainedPrice")
                (Some(comissionPrice), Some(retainedPrice))
              } else {
                // Все деньги уходят узлу-ресиверу.
                LOGGER.warn(s"$logPrefix Comission==0, ALL money going to node#$rcvrId")
                (None, Some(mitem.price))
              }
            }

            // Подготовить данные по всем бенефициарам:
            resp <- Future.traverse {
              (bill2Conf.CBCA_NODE_ID, cbcaComissedPriceOpt0, true) ::
              (rcvrId, rcvrPriceOpt0, false) ::
              Nil
            } { case (nodeId, priceOptRaw, isSioComission) =>
              // Чтобы гарантировано не было неправильных или нулевых транзакций, фильтруем результаты
              val priceOpt = priceOptRaw
                .filter(_.amount > 0)
              FutureUtil.optFut2futOpt(priceOpt) { mprice =>
                for {
                  enc <- prepareMoneyReceiver(nodeId)
                } yield {
                  val mri = MoneyRcvrData( mprice, enc, isSioComission, items )
                  Some( mri )
                }
              }
            }

          } yield {
            val resp2 = resp.flatten
            LOGGER.debug(s"$logPrefix Finally, found ${resp2.length} money-rcvrs: ${resp2.iterator.map{ mri => s"${mri.price} -> c#${mri.enc.contract.id.orNull} node#${mri.enc.mnode.idOrNull}" }.mkString("\n ")}")
            resp2
          }
        }
    }
  }


  def mergeMoneyReceiversByNodeId( moneyRcvrsLists: Iterable[MoneyRcvrData] ): Map[String, Iterable[MoneyRcvrData]] = {
    lazy val logPrefix = s"mergeMoneyReceiversByNodeId(${moneyRcvrsLists.size}):"

    val rcv_price_amount_LENS = MoneyRcvrData.price
      .andThen( MPrice.amount )

    moneyRcvrsLists
      .groupBy( _.enc.mnode.id.get )
      .map { case (nodeId, rcvInfos) =>
        val rcvrInfos2 = rcvInfos
          .groupMapReduce { rcvInfo =>
            rcvInfo.enc.contract.id -> rcvInfo.price.currency
          } ( identity ) { (rcvInfo0, rcvInfo1) =>
            rcv_price_amount_LENS.modify( _ + rcvInfo1.price.amount )( rcvInfo0 )
          }
          .values

        LOGGER.trace(s"$logPrefix For node#$nodeId price sums reduces: ${rcvInfos.size}=>${rcvrInfos2.size}")
        nodeId -> rcvrInfos2
      }
  }


  /** Is it allowed to launch external deal for semi-direct money transfers between payer and seller.
    * @param moneyRcvrs Result of [[prepareMoneyReceivers]] or other moneyRcvrInfos.
    * @return
    */
  def canPayExtTxn(paySystem: MPaySystem, moneyRcvrs: Iterable[MoneyRcvrData]): ExtDealPreDecision = {
    val (can, cant) = moneyRcvrs
      .partition { rcvInfo =>
        rcvInfo.enc.mnode.edges
          .withPredicateIter( MPredicates.PayoutData )
          .exists { e =>
            e.info.paySystem
              .fold( true )( _ ==* paySystem )
          }
      }

    ExtDealPreDecision(
      dealWith      = can,
      outOfDeal     = cant,
    )
  }


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

    (for {
      // Получить и заблокировать обрабатываемый item.
      mitem0 <- _prepareAwaitingItem(itemId)

      // Запустить В ФОНЕ вне транзакции сбор базовой инфы о получателе денег. "mr" означает "Money Receiver".
      mrsFut = prepareMoneyReceivers(mitem0)

      orderOpt <- mOrders.getById( mitem0.orderId )
      order = orderOpt.get

      // Получить и заблокировать баланс покупателя
      balanceOpt0 <- mBalances
        .getByContractCurrency( order.contractId, mitem0.price.currency )
        .forUpdate
      balance0 = balanceOpt0.get

      // Списать blocked amount с баланса покупателя
      usrAmtBlocked2 <- mBalances.incrBlockedBy(balance0.id.get, -mitem0.price.amount)

      // Запустить обновление полностью на стороне БД.
      mitem2 <- {
        LOGGER.debug(s"$logPrefix Buyer blocked balance[${balance0.id.orNull}] freed ${mitem0.price.amount}: ${balance0.blocked} => ${usrAmtBlocked2.orNull} ${balance0.price.currency}")
        val status2 = approvedItemStatus( mitem0.iType )
        val dateStatus2 = OffsetDateTime.now()

        for {
          rowsUpdated <- mItems.query
            .filter(_.id === itemId)
            .map { i =>
              (i.status, i.dateStatus)
            }
            .update((status2, dateStatus2))
          if rowsUpdated ==* 1
        } yield {
          LOGGER.trace(s"$logPrefix item#$itemId updated status => $status2")
          mitem0.copy(
            status      = status2,
            dateStatus  = dateStatus2,
          )
        }
      }

      // Check, if there are payouts after items processing:
      isDealOrder = order.status ==* MOrderStatuses.Paid
      // If this is a deal, let's check info about possible payout for current item.
      itemPayoutTxnIdOpt <- if (isDealOrder) {
        mTxns
          .query
          .filter { tx =>
            (tx.itemIdOpt === itemId) &&
            (tx.txTypeStr === MTxnTypes.Payout.value) &&
            (tx.datePaidOpt.isEmpty)
          }
          .map(_.id)
          .take(1)
          .result
          .headOption
      } else {
        DBIO.successful( None )
      }

      mrs <- DBIO.from( mrsFut )

      // Организовать зачисление денег на счета бенефициаров:
      _ <- {
        LOGGER.trace(s"$logPrefix item[${mitem2.id.orNull}] status: ${mitem0.status} => ${mitem2.status}, order#${mitem0.orderId} isDealOrder?$isDealOrder payoutTxn#${itemPayoutTxnIdOpt.orNull}")

        if (mrs.isEmpty) {
          LOGGER.warn(s"$logPrefix Money income skipped, so they are lost.")
          DBIO.successful(None)
        } else {
          // Есть бенефициары, зачислить им всем на балансы.
          val now = OffsetDateTime.now()
          val actions = for (mr <- mrs) yield {
            val contractId = mr.enc.contract.id.get
            for {
              // Найти/создать кошелек получателя денег
              mrBalance0    <- ensureBalanceFor( contractId, mr.price.currency )
              // Зачислить деньги на баланс.
              // TODO В будущем, когда будет нормальная торговля между юзерами, надо будет проводить какие-то транзакции на стороне seller'а.
              mrAmount2Opt  <- {
                if (itemPayoutTxnIdOpt.isDefined && !mr.isSioComissionPrice) {
                  LOGGER.trace(s"$logPrefix Ignoring money-receiver balance, because item#$itemId of price=${mr.price} already have payout txn#${itemPayoutTxnIdOpt.orNull}, and money receiver#${mr.enc.mnode.id.orNull} contract#$contractId is not Suggest.io comission target")
                  //mBalances.incrBlockedBy( mrBalanceId, mrBalanceAmountDelta )
                  DBIO.successful( None )
                } else {
                  val mrBalanceId = mrBalance0.id.get
                  val mrBalanceAmountDelta = mr.price.amount
                  for {
                    mrAmount2 <- mBalances.incrAmountBy( mrBalanceId, mrBalanceAmountDelta )
                    // Save transaction for receiver's balance:
                    mrIncrTxn <- mTxns insertOne MTxn(
                      balanceId   = mrBalanceId,
                      amount      = mrBalanceAmountDelta,
                      txType      = MTxnTypes.Income,
                      orderIdOpt  = Some( mitem0.orderId ),
                      itemId      = Some( itemId ),
                      // TODO dateStatus of item may not equal to real datePaid. Maybe need to fix this?
                      datePaid    = Some( mitem0.dateStatus ),
                      dateProcessed = now,
                    )
                  } yield {
                    LOGGER.trace(s"$logPrefix Created txn#${mrIncrTxn.id.orNull} for income ${mr.price} to balance#${mrBalanceId} of contract#$contractId node#${mr.enc.mnode.id.orNull}")
                    mrAmount2
                  }
                }
              }
              // seller-транзакцию не создаём, т.к. она на раннем этапе не нужна: будет куча ненужного мусора в txn-таблице.
            } yield {
              // foreach в виде map, т.к. DBIO не подразумевает foreach
              val mrAmount2 = mrAmount2Opt.get
              LOGGER.debug(s"$logPrefix Money receiver[${mr.enc.mnode.id.orNull}] contract[${mr.enc.contract.id.orNull}] balance[${mrBalance0.id.orNull}] updated: ${mrBalance0.blocked} + ${mr.price.amount} => $mrAmount2 ${mrBalance0.price.currency}")
            }
          }
          DBIO.seq(actions: _*)
        }
      }

    } yield {
      LOGGER.trace(s"$logPrefix Done ok.")
      ApproveItemResult( mitem2 )
    })
      .transactionally
  }


  /**
    * Найти/создать баланс для указанного контракта и валюты.
    *
    * @param contractId id контракта.
    * @param currency Валюта баланса.
    * @return Экшен, возвращающий баланс, готовый к обновлению.
    */
  def ensureBalanceFor(contractId: Gid_t, currency: MCurrency): DBIOAction[MBalance, NoStream, RWT] = {
    lazy val logPrefix = s"ensureBalanceFor($contractId,$currency):"
    (for {
      // Считать баланс получателя...
      balanceOpt <- {
        mBalances
          .getByContractCurrency(contractId, currency)
          .forUpdate
      }

      // Если баланс отсутствует, то инициализировать
      balance0 <- {
        balanceOpt.fold [DBIOAction[MBalance, NoStream, RW]] {
          for {
            bal2 <- mBalances.initByContractCurrency(contractId, currency)
          } yield {
            LOGGER.debug(s"$logPrefix Initialized new balance#${bal2.id.orNull}: $bal2")
            bal2
          }
        } { DBIO.successful }
      }

    } yield {
      balance0
    })
      .transactionally
  }


  /**
    * Item не прошел модерацию или продавец/поставщик отказал по какой-то [уважительной] причине.
    *
    * @param itemId id итема, от которого отказывается продавец.
    * @param reasonOpt причина отказа, если есть.
    * @return
    */
  def refuseItem(itemId: Gid_t, reasonOpt: Option[String]): DBIOAction[RefuseItemResult, NoStream, RWT] = {
    lazy val logPrefix = s"refuseItem($itemId):"
    (for {
      // Получить и заблокировать текущий item.
      mitem0 <- _prepareAwaitingItem(itemId)

      // Получить и заблокировать баланс юзера
      balance0 <- _item2balance(mitem0)

      // Разблокировать на балансе сумму с этого item'а
      // TODO What about external deals? transactions for deals? and no money on balance.
      amount2 <- mBalances.incrAmountAndBlockedBy(balance0, mitem0.price.amount)

      // Отметить item как отказанный в размещении
      mitem2 <- {
        LOGGER.debug(s"$logPrefix Unlocked user balance[${balance0.id.orNull}] amount ${mitem0.price}: ${balance0.price} => ${amount2.orNull} ${balance0.price.currency}")

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
          .filter(_ ==* 1)
          .map(_ => mi2)
      }

      // Провести транзакцию по движению на счете
      mtxn2 <- {
        LOGGER.trace(s"$logPrefix Refused item, status ${mitem0.status} => ${mitem2.status}, reason = $reasonOpt")
        val mtxn0 = MTxn(
          balanceId  = balance0.id.get,
          amount     = mitem0.price.amount,
          txType     = MTxnTypes.ReturnToBalance,
          itemId     = Some(itemId)
        )
        mTxns.insertOne(mtxn0)
      }

    } yield {
      LOGGER.trace(s"$logPrefix Saved MTxn[${mtxn2.id.orNull}]")
      RefuseItemResult(mitem2, mtxn2)
    })
      .transactionally
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
  def justFinalizeItems(itemIds   : Iterable[Gid_t],
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
                            iTypes    : Iterable[MItemType],
                            statuses  : Iterable[MItemStatus]         = Nil,
                            rcvrIds   : Iterable[String]              = Nil,
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
          i0 && i.withRcvrIds(rcvrIds)
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
      .take( 1 )

    // Найти contractId указанного ордера.
    mOrders.query
      .filter(_.id in itemOrderIdsQ)
      .map(_.contractId)
      .result
      .headOption
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
        val currency = MCurrencies.withValue(currencyCode)
        MPrice(amount, currency)
      }
      pricesIter.toSeq
    }
  }


  /**
    * Сборка ценников для списка ордеров.
    *
    * @param orderIds Список ордеров, которые надо обсчитать.
    * @return Карта ценников по ордерам.
    */
  def getOrdersPrices(orderIds: Iterable[Gid_t]): DBIOAction[Map[Gid_t, Seq[MPrice]], NoStream, Effect.Read] = {
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
              currency  = MCurrencies.withValue(currencyCode)
            )
            orderId -> price
          }
        }
        .toSeq
        .groupBy(_._1)
        // Выкинуть orderId из полученных значений.
        .view
        .mapValues { orderPrices =>
          for ( (_, p) <- orderPrices )
          yield p
        }
        .toMap
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
      prices.iterator.map(_.currency).toSet.size ==* prices.size,
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
  def getPayableOrderForUpdate(orderId: Gid_t, validContractId: Gid_t): DBIOAction[MOrder, NoStream, Effect.Read] = {
    for {
      // Прочитать запрошенный ордер, одновременно проверяя необходимые поля.
      mOrderOpt <- {
        mOrders.query
          .filter { o =>
            (o.id === orderId) &&
            (o.contractId === validContractId) &&
            (o.statusStr inSet MOrderStatuses.canGoToPaySys.onlyIds.to(Iterable))
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
    *
    * @param orderId Заявленный платежной системой order_id. Ордер будет проверен на связь с контрактом.
    * @param validContractId Действительный contract_id юзера, выверенный по максимуму.
    * @param claimedOrderPrices Цены заказа по мнению платежной системы.
    *                           Map: валюта -> цена.
    * @return DB-экшен.
    */
  def checkPayableOrder(orderId: Gid_t, validContractId: Gid_t, paySys: MPaySystem,
                        claimedOrderPrices: Map[MCurrency, MPrice]): DBIOAction[MOrder, NoStream, RWT] = {
    lazy val logPrefix = s"checkPayableOrder(o=$orderId,c=$validContractId):"

    (for {

      // Прочитать и проверить запрошенный ордер.
      mOrder <- getPayableOrderForUpdate(orderId, validContractId = validContractId)

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
      mOrder
    })
      .transactionally
  }


  /** Захолдить ордер, если ещё не захолжен.
    *
    * @param mOrder Исходный ордер.
    * @return DB-экшен, возвращающий обновлённый ордер.
    */
  def holdOrder(mOrder: MOrder): DBIOAction[MOrder, NoStream, Effect.Write] = {
    val holdStatus = MOrderStatuses.Hold
    def logPrefix = s"holdOrder(${mOrder.id.orNull}):"
    if (mOrder.status ==* holdStatus) {
      LOGGER.debug(s"$logPrefix Not holding order, because already hold since ${mOrder.dateStatus}: $mOrder")
      DBIO.successful(mOrder)
    } else {
      // Ордер не является замороженным. Заменяем ему статус.
      val o2 = (MOrder.status replace holdStatus)( mOrder )
      for {
        rowsUpdated <- mOrders.saveStatus( o2 )
        if rowsUpdated ==* 1
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
  def unholdOrder(orderId: Gid_t, comment: Option[String] = None): DBIOAction[(MOrder, Seq[MTxn]), NoStream, RWT] = {
    lazy val logPrefix = s"unHoldOrder($orderId)[${System.currentTimeMillis()}]:"

    (for {
      // Прочитать новый ордер в рамках транзакции.
      morderOpt <- mOrders.getById(orderId).forUpdate
      morder = morderOpt.get

      // Выполнять какие-то действия, только если позволяет текущий статус ордера.
      res <- {
        if (morder.status ==* MOrderStatuses.Hold) {
          val morder22 = (MOrder.status replace MOrderStatuses.Draft)( morder )
          for {
            // Поискать текущую корзину, вдруг юзер ещё что-то в корзину швырнул, пока текущий ордер был HOLD.
            cartOrderOpt    <- getCartOrder(morder.contractId)
              .forUpdate

            now = OffsetDateTime.now()
            ordersUpdated <- mOrders.saveStatus( morder22, now )
            if ordersUpdated ==* 1

            txns0 <- mTxns.query
              .filter { txn =>
                (txn.orderIdOpt === orderId) &&
                (txn.txTypeStr === MTxnTypes.Draft.value)
              }
              .result

            // Mark all pending order transactions as cancelled:
            // Updating one-by-one txn, because only 1 txn here (at maximum), so it is not a performance fail here.
            txnsUpdated <- DBIO.sequence {
              for {
                txn <- txns0
                txn2 = txn.copy(
                  txType          = MTxnTypes.Cancelled,
                  dateProcessed   = now,
                  paymentComment  = comment,
                )
              } yield {
                mTxns
                  .saveTypeAndComment( txn2 )
                  .map { countUpdated =>
                    if (countUpdated !=* 1)
                      LOGGER.warn(s"$logPrefix txn#${txn.id} => Unexpected updated rows count [$countUpdated], must be 1. Txn: $txn2")
                    txn2
                  }
              }
            }

            // Текущий ордер теперь снова стал корзиной.
            // Но возможно, что уже существует ещё одна корзина? Их надо объеденить в пользу текущего ордера.
            _ <- {
              LOGGER.trace(s"$logPrefix ${txnsUpdated.size} draft txns#[${txnsUpdated.iterator.map(_.id.fold("?")(_.toString)).mkString(",")}] marked as cancelled.")
              cartOrderOpt.fold [DBIOAction[_, NoStream, WT]] {
                // Нет внезапной корзины, всё ок.
                LOGGER.trace(s"$logPrefix All ok, no duplicating cart-orders found.")
                DBIO.successful(())
              } { suddenCartOrder =>
                // Обнаружена внезапная корзина. Переместить все item'ы из неё в текущий ордер, и удалить внезапную корзину.
                val suddenCartOrderId = suddenCartOrder.id.get
                val orderDepsMovedCountAction = mergeOrders( suddenCartOrderId, toOrderId = orderId )
                LOGGER.info(s"$logPrefix Will merge cart orders $suddenCartOrderId => ${morder.id.orNull}, because new cart already exists.")
                for (movedCount <- orderDepsMovedCountAction) yield {
                  LOGGER.debug(s"$logPrefix Moved $movedCount order deps from sudden cart#$suddenCartOrderId into cancelled order#$orderId")
                }
              }
            }

          } yield {
            // Вернуть обновлённый инстанс.
            morder22 -> txnsUpdated
          }

        } else if (morder.status ==* MOrderStatuses.Draft) {
          LOGGER.debug(s"$logPrefix Already draft since ${morder.dateStatus}. Nothing to do, skipped.")
          DBIO.successful( morder -> Nil )
        } else {
          val msg = s"$logPrefix Order status is ${morder.status}, so cannot unhold. Only holded orders or drafts are allowed."
          LOGGER.error(msg)
          DBIO.failed( new IllegalArgumentException(msg) )
        }
      }

    } yield {
      LOGGER.debug(s"$logPrefix Order now draft. Was HOLD.")
      res
    })
      .transactionally
  }


  /** Объединение двух ордеров в пользу последнего из двух.
    *
    * @param orderIdForDelete id ордера, который подлежит уничтожению с переносом всех зависимых в новый ордер.
    * @param toOrderId Целевой ордер, в который будет происходить перенос.
    * @return DB-экшен, возвращающий кол-во удалённых ордеров. Т.е. или 1, или NSEE.
    */
  def mergeOrders(orderIdForDelete: Gid_t, toOrderId: Gid_t): DBIOAction[Int, NoStream, WT] = {
    lazy val logPrefix = s"mergeOrders($orderIdForDelete => $toOrderId)[${System.currentTimeMillis()}]:"
    (for {

      // Обновить все item'ы, транзакции и прочее.
      depRowsUpdated <- moveOrderDeps(orderIdForDelete, toOrderId = toOrderId)

      // Теперь удалить старую корзину. Список транзакций не проверяем, т.к. это же DRAFT-ордер, транзакций у таких не бывает.
      ordersDeleted <- {
        LOGGER.info(s"$logPrefix $depRowsUpdated order deps moved from oldOrder#$orderIdForDelete to target order#$toOrderId. Deleting old order...")
        mOrders.deleteById( orderIdForDelete )
      }
      if ordersDeleted ==* 1

      // Удалить любые дебажные вещи, касающиеся удаляемого ордера
      orderDebugsDeleted <- mDebugs.deleteByObjectId( orderIdForDelete )

    } yield {
      LOGGER.info(s"$logPrefix Deleted $ordersDeleted cart-order with $orderDebugsDeleted order's debugs.")
      ordersDeleted
    })
      .transactionally
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
      LOGGER.debug(s"moveOrderDeps($fromOrderId => $toOrderId): itemsUpdated=${itemsUpdated} txnsUpdated=$txnsUpdated")
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
          (o.statusStr === MOrderStatuses.Hold.value) && (o.dateStatus < oldNow)
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
  def incrUserBalanceFromPaySys(contractId: Gid_t, mprice: MPrice, psTxnUid: String, orderIdOpt: Option[Gid_t] = None, paySystem: MPaySystem,
                                txType: MTxnType, comment: Option[String] = None, txn: Option[MTxn] = None): DBIOAction[(MBalance, MTxn), NoStream, RWT] = {
    lazy val logPrefix = s"incrUserBalanceFromPaySys($psTxnUid, $contractId/${orderIdOpt.orNull}, $mprice):"
    LOGGER.trace(s"$logPrefix Starting; comment=${comment.orNull}")

    (for {
      // Find/ensure user's balance for currency...
      usrBalance0 <- ensureBalanceFor( contractId, mprice.currency )

      // Search for already-create (opened and not yet paid) transaction in db:
      txnOpt0 <- txn.fold [DBIOAction[Option[MTxn], NoStream, Effect.Read]] {
        mTxns
          .query
          .filter { t =>
            (t.psTxnUidOpt === psTxnUid) &&
            (t.paySystemStr === paySystem.value)
          }
          // ignoring datePaid.isEmpty and else: It will be checked below.
          .take( 1 )
          .result
          .headOption
      } { _ =>
        DBIO.successful( txn )
      }

      if txnOpt0.fold {
        LOGGER.trace(s"$logPrefix No previously-opened transaction found.")
        true
      } { txn0 =>
        LOGGER.debug(s"$logPrefix Found previously-created transaction#${txn0.id.orNull} with amount=${txn0.amount} for balance#${txn0.balanceId}")
        val r = {
          txn0.datePaid.isEmpty &&
          (txn0.amount ==* mprice.amount) &&
          (usrBalance0.id contains[Gid_t] txn0.balanceId)
        }
        if (!r) LOGGER.error(s"$logPrefix Transaction#${txn0.id.orNull} re-payment with same id#${txn0.psTxnUidOpt.orNull}. WTF? Already processed tx @${txn0.datePaid} balance#${txn0.balanceId} amount[${txn0.amount} vs $mprice]")
        r
      }

      // Increment balance with received money:
      usrBalance2Opt <- mBalances.incrAmountBy( usrBalance0, mprice.amount )
      usrBalance2 = usrBalance2Opt.get

      // Insert/update database transaction about money received:
      balIncrTxn <- {
        LOGGER.trace(s"$logPrefix Incremented user balance#${usrBalance2.id.orNull}: ${usrBalance0.price} => ${usrBalance2.price}")
        val datePaid = Some( OffsetDateTime.now() )

        txnOpt0.fold {
          LOGGER.trace(s"$logPrefix Creating/inserting new txn...")
          val txn0 = MTxn(
            balanceId       = usrBalance2.id.get,
            amount          = mprice.amount,
            txType          = txType,
            orderIdOpt      = orderIdOpt,
            paymentComment  = comment,
            psTxnUidOpt     = Some( psTxnUid ),
            datePaid        = datePaid,
            paySystem       = Some( paySystem ),
          )
          mTxns.insertOne( txn0 )

        } { txnExisting =>
          LOGGER.trace(s"$logPrefix Updating exising txn#${txnExisting.id.orNull} with datePaid=$datePaid ...")
          val txnFinal = txnExisting.copy(
            datePaid        = datePaid,
            paymentComment  = comment,
            txType          = txType,
          )
          mTxns.updatePaidInfo( txnFinal )
        }
      }

    } yield {
      LOGGER.debug(s"incrUserBalanceFromPaySys($contractId, $mprice, $psTxnUid, $orderIdOpt): done, comment=$comment")
      (usrBalance2, balIncrTxn)
    })
      .transactionally
  }


  private def _getOrderTxnsQuery(orderId: Gid_t) = {
    mTxns.query
      .filter { t =>
        (t.orderIdOpt === orderId)
      }
  }


  /** Найти в базе платежную транзакцию, относящуюся к указанному заказу.
    * Т.е. транзакцию зачисления денег из платежной системы в sio-биллинг.
    *
    * @param orderId Номер заказа.
    * @return DB-экшен с опциональной транзакцией.
    */
  def getOrderTxns(orderId: Gid_t): DBIOAction[Seq[MTxn], Streaming[MTxn], Effect.Read] = {
    _getOrderTxnsQuery( orderId )
      // Если вдруг больше одной транзакции, то интересует самая ранняя. Такое возможно из-за логических ошибок при аппруве item'ов.
      .sortBy(_.id.asc)
      .result
  }


  /** Получить транзакции с валютами.
    *
    * @param orderId id заказа, к которому относятся транзакции.
    * @return DB-экшен, возвращающий MTxn -> MCurrency.
    */
  def getOrderTxnsWithCurrencies(orderId: Gid_t): DBIOAction[(Seq[MTxn], Map[Gid_t, MCurrency]), NoStream, Effect.Read] = {
    lazy val logPrefix = s"getOrderTxnsWithCurrencies($orderId):"
    for {
      // List transactions for order:
      txns <- _getOrderTxnsQuery( orderId ).result

      // List related balances:
      balances2Currency: Map[Gid_t, MCurrency] <- {
        if (txns.isEmpty) {
          DBIO successful Map.empty[Gid_t, MCurrency]
        } else {
          // Collect currencies information. Usually, only one currency is here
          val balanceIds = txns
            .iterator
            .map( _.balanceId )
            .toSet

          mBalances.query
            .filter(_.id inSet balanceIds)
            .map { b =>
              (b.id, b.currency)
            }
            .result
            .map(_.toMap)
        }
      }
    } yield {
      LOGGER.trace(s"$logPrefix Found ${txns.length} transactions, ${balances2Currency.size} balances/currencies.")
      txns -> balances2Currency
    }
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
          group.map(_.statusStr).max =!= MItemStatuses.AwaitingMdr.value
        )
      }
      // LIMIT 200
      .take(limit)
      .result
    // TODO Нужно завернуть кортежи в MAdvGeoShapeInfo. .map() не котируем, т.к. ломает streaming.
  }


  /** Есть ли прямые размещения на указанном узле?
    *
    * @param nodeId id целевого узла.
    * @return true, если есть хотя бы один busy adv item прямого размещения на указанном узле.
    */
  def hasAnyBusyToNode(nodeId: String): DBIOAction[Boolean, NoStream, Effect.Read] = {
    mItems.query
      .filter { i =>
        i.withNodeId( nodeId ) &&
          i.withStatuses( MItemStatuses.advBusy ) &&
          i.withTypes1( MItemTypes.advDirectTypes: _* )
      }
      .exists
      .result
  }


  /** Если база контрактов вдруг потерялась или содержит сомнения, то нужно пройтись по узлам и проверить
    * contract_id узлов на предмет наличия существующего контракта в contracts.
    *
    * @return Фьючерс.
    */
  def fsckNodesContracts(): Future[(Int, Int)] = {
    import mNodes.Implicits._

    lazy val logPrefix = s"fsckNodesContracts()#${System.currentTimeMillis()}:"
    val someTrue = Some(true)

    // Для ускорения - собрать множество всех id для контрактов в базе. TODO Memory, на большой базе может не хватить RAM.
    val allContractIdsFut = slick.db.stream {
      mContracts.query
        .map(_.id)
        .result
    }
      .toSource
      .toMat(
        Sink.collection[Gid_t, Set[Gid_t]]
      )(Keep.right)
      .run()

    // Не ясно, даёт ли вынос source() за пределы for ускорение. По идее - нет.
    val src0 = mNodes.source[MNode](
      searchQuery = new MNodeSearch {
        override def contractIdDefined = someTrue
      }.toEsQuery
    )

    val bp = mNodes.bulkProcessorLog( this, logPrefix )
    val totalCounter = new AtomicInteger(0)
    val repairCounter = new AtomicInteger(0)

    for {
      allContractIds <- allContractIdsFut
      _ = LOGGER.debug(s"$logPrefix Found ${allContractIds.size} contracts.")

      _ <- src0.runForeach { mnode =>
        mnode.billing.contractId.fold[Any] {
          LOGGER.warn(s"$logPrefix Node#${mnode.idOrNull} missing contract id, but expected")
        } { contractId =>
          if (allContractIds contains contractId) {
            LOGGER.trace(s"$logPrefix Node#${mnode.idOrNull}, contract#${contractId} OK")
          } else {
            LOGGER.info(s"$logPrefix Repair node#${mnode.idOrNull}, contract#${contractId} NOT exist.")
            val mnode2 = mNodes.prepareIndex(
              MNode.billing
                .andThen( MNodeBilling.contractId )
                .replace( None )(mnode)
            )
            bp.add( mnode2.request() )
            repairCounter.incrementAndGet()
          }
        }
        totalCounter.incrementAndGet()
      }
    } yield {
      bp.close()
      val repaired = repairCounter.intValue()
      val total = totalCounter.intValue()
      LOGGER.info(s"$logPrefix Repaired $repaired of $total")
      (repaired, total)
    }
  }


  /** Open & save not-yet-paid transaction.
    * Payment will be done in nearest future from pay-system, and this transaction will updated & closed.
    */
  def openPaySystemTxn(paySystem: MPaySystem, balanceId: Gid_t, payAmount: Amount_t, orderIdOpt: Option[Gid_t],
                       psTxnId: String, psDealIdOpt: Option[String], txnMetadata: Option[MTxnMetaData] = None): DBIOAction[MTxn, NoStream, Effect.Write] = {
    mTxns insertOne MTxn(
      balanceId   = balanceId,
      amount      = payAmount,
      txType      = MTxnTypes.Draft,
      orderIdOpt  = orderIdOpt,
      psTxnUidOpt = Some( psTxnId ),
      datePaid    = None,
      paySystem   = Some( paySystem ),
      metadata    = txnMetadata,
      psDealIdOpt = psDealIdOpt,
    )
  }


  /** Hi-level DB-action for received money from pay-system.
    * Process payment and process cart order.
    * This db-action is used after pay-system payment is received. */
  def handlePaymentReceived(orderId: Gid_t, contractId: Gid_t, payPrice: MPrice, paySysUid: String, paySystem: MPaySystem,
                            comment: Option[String] = None, txn: Option[MTxn] = None): DBIOAction[PaymentResult, NoStream, RWT] = {
    lazy val logPrefix = s"handlePaymentReceived($orderId, $payPrice, $paySysUid):"
    LOGGER.trace(s"$logPrefix Starting, order#$orderId contract#$contractId payPrice=$payPrice paySysUid[$paySysUid]: ${comment.orNull}")

    (for {
      // Read currently draft/hold order for update:
      mOrder0  <- getPayableOrderForUpdate( orderId, validContractId = contractId )

      // Save payment money on balance + transaction.
      (balance2, balanceTxn) <- incrUserBalanceFromPaySys(
        contractId  = contractId,
        mprice      = payPrice,
        orderIdOpt  = Some( orderId ),
        psTxnUid    = paySysUid,
        comment     = comment,
        paySystem   = paySystem,
        txn         = txn,
        txType      = MTxnTypes.Payment,
      )

      // If order is hold, it's items must be processed:
      r <- if (mOrder0.status ==* MOrderStatuses.Hold) {
        for {
          // Read all draft order's items:
          draftOrderWithItems <- orderWithDraftItems( mOrder0 )

          // Process order items step-by-step. For each item, substract some money from balance, execute related actions.
          // Here, complex-logic finalizer is used (for tolerating possible problems, if money received != order price).
          // Fallback order finalizer can be used: maybeExecuteOrder(owi).
          finOrderRes <- forceFinalizeOrder( draftOrderWithItems )
        } yield {
          LOGGER.trace(s"$logPrefix Completed, txn#${balanceTxn.id.orNull} order#$orderId=>${finOrderRes.closedOrder.status} items[${draftOrderWithItems.items.length}/${finOrderRes.okItemsCount}]")
          PaymentResult( draftOrderWithItems, balanceTxn, finOrderRes )
        }

      } else {
        LOGGER.trace(s"$logPrefix Order#${mOrder0.id.orNull} status#${mOrder0.status} is not HOLD. Do NOT processing any of order items.")
        val payRes = PaymentResult(
          MOrderWithItems( mOrder0, Nil ),
          balanceTxn,
          ForceFinalizeOrderRes(
            mOrder0,
            skippedCartOpt = Option.when( mOrder0.status ==* MOrderStatuses.Draft )(mOrder0),
            newBalances = Map.empty + (balance2.currency -> balance2),
            okItemsCount = 0,
          )
        )
        DBIO successful payRes
      }

    } yield {
      r
    })
      .transactionally
  }


  /** Check, if all order items are already passed Draft or AwaitMdr statuses, and now are in some next statuses. */
  def isAllOrderItemsProcessed(orderId: Rep[Gid_t]): Rep[Boolean] = {
    mItems
      .query
      .filter { item =>
        (item.orderId === orderId)
      }
      .filterNot { item =>
        val forbidStatuses = MItemStatuses.AwaitingMdr.value :: MItemStatuses.Draft.value :: Nil
        (item.statusStr inSet forbidStatuses)
      }
      .exists
  }

  /** Query for grepping transactions, awaiting for payout. */
  def orderTxnsWaitingPayout(orderId: Rep[Gid_t]): Query[mTxns.MTxnsTable, MTxn, Seq] = {
    mTxns
      .query
      .filter { tx =>
        (tx.orderIdOpt === orderId) &&
        (tx.txTypeStr === MTxnTypes.Payout.value) &&
        (tx.datePaidOpt.isEmpty)
      }
  }

  /** Find ordmTxns
          .query
          .filter { tx =>
            (tx.orderIdOpt === orderId) &&
            (tx.txTypeStr === MTxnTypes.Payout.value) &&
            (tx.datePaidOpt.isEmpty)
          }ers, awaiting for payout action (everything else is ready). */
  def findPaidReadyToPayoutOrders(): DBIOAction[Seq[Gid_t], Streaming[Gid_t], Effect.Read] = {
    mOrders
      .query
      .filter { order =>
        (order.statusStr === MOrderStatuses.Paid.value)
      }
      .map(_.id)
      .filter {
        // Order have all items processed?
        isAllOrderItemsProcessed
      }
      .filter { orderId =>
        // Order have incomplete payouts?
        orderTxnsWaitingPayout( orderId ).exists
      }
      .result
  }


  /** Open draft payout transactions for deal money receivers.
    *
    * @param orderIdOpt id of current cart order.
    * @param paySystem payment system id.
    * @param psDealId PaySystem-side deal.
    * @param moneyRcvrs Money receivers list.
    * @return All created transactions.
    */
  def openDealPayoutTxns(orderIdOpt: Option[Gid_t], paySystem: MPaySystem, psDealId: String,
                         moneyRcvrs: Iterable[MoneyRcvrData] ): DBIOAction[Iterable[MTxn], NoStream, RWT] = {
    lazy val logPrefix = s"openPayoutDealTxn(order#${orderIdOpt.orNull}, $paySystem#$psDealId, [${moneyRcvrs.size}]):"

    val psDealIdSome = Some( psDealId )
    val paySystemSome = Some( paySystem )
    val now = OffsetDateTime.now()

    DBIO
      .sequence(
        moneyRcvrs
          .groupBy { mrd =>
            mrd.enc.contract.id.get -> mrd.price.currency
          }
          .map { case ((contractId, currency), contractMoneyRcvrs) =>
            for {
              balance <- ensureBalanceFor( contractId, currency )
              txns <- DBIO.sequence {
                for {
                  moneyRcvr <- contractMoneyRcvrs
                } yield {
                  for {
                    txn <- mTxns insertOne MTxn(
                      balanceId   = balance.id.get,
                      amount      = moneyRcvr.price.amount,
                      itemId      = moneyRcvr.items
                        .headOption
                        .flatMap(_.id),
                      txType      = MTxnTypes.Payout,
                      orderIdOpt  = orderIdOpt,
                      paySystem   = paySystemSome,
                      psDealIdOpt = psDealIdSome,
                      dateProcessed = now,
                      datePaid    = None,
                    )
                  } yield {
                    LOGGER.trace(s"$logPrefix Opened draft payout txn#${txn.id.orNull} for ${moneyRcvr.price} to balance#${balance.id.orNull} of contact#$contractId of node#${moneyRcvr.enc.mnode.id.orNull}")
                    txn
                  }
                }
              }
            } yield {
              txns
            }
          }
      )
      .transactionally
      // Lazy flatten, because these transactions are mostly unused.
      .map( _.iterator.flatten.to(LazyList) )
  }

}


object Bill2Util {

  sealed case class PaymentResult(
                                   draftOrder     : MOrderWithItems,
                                   balanceTxn     : MTxn,
                                   result         : ForceFinalizeOrderRes,
                                 )

  sealed case class EnsuredNodeContract( contract: MContract, mnode: MNode ) {
    override def toString: String = StringUtil.toStringHelper("") { f =>
      val noNameF = f("")
      noNameF( contract.id.orNull )
      noNameF( mnode.id.orNull )
    }
  }

  sealed case class ForceFinalizeOrderRes(
                                          closedOrder         : MOrder,
                                          skippedCartOpt      : Option[MOrder],
                                          newBalances         : Map[MCurrency, MBalance],
                                          okItemsCount        : Int
                                         )

  /** Модель инфы по получателю денег: кол-во денег и описание узла с контрактом.
    *
    * @param price Цена, которая отходит указанному бенефициару.
    * @param enc Данные по узлу и его контракту.
    * @param isSioComissionPrice true - this money exclusively goes into s.io.
    *                            false - this money goes to ad placement owner (ble beacon owner, etc).
    *                            None - Uncertain. For example, several different prices are merged into one.
    */
  final case class MoneyRcvrData(
                                  price : MPrice,
                                  enc   : EnsuredNodeContract,
                                  isSioComissionPrice: Boolean,
                                  items : List[MItem],
                                ) extends IMPrice {
    override def toString: String = StringUtil.toStringHelper("") { f =>
      val noNameF = f("")
      noNameF( price )
      noNameF( enc )
      f("commission?")(isSioComissionPrice)
    }
  }
  object MoneyRcvrData {
    def price = GenLens[MoneyRcvrData](_.price)
  }

  /** Контейнер результата экшена аппрува item'а. */
  sealed case class ApproveItemResult(override val mitem: MItem)
    extends IMItem

  /** Результат исполнения экшена refuseItemAction(). */
  sealed case class RefuseItemResult(override val mitem: MItem, mtxn: MTxn)
    extends IMItem


  /** Container of external dealing result.
    *
    * @param dealWith Deal direct transaction may be launched against these public.
    * @param outOfDeal No possible deal with rest of money-receivers.
    */
  case class ExtDealPreDecision(
                                 dealWith          : Iterable[MoneyRcvrData],
                                 outOfDeal         : Iterable[MoneyRcvrData],
                               )


  case class ExtDealPrepareResult(
                                   decision: ExtDealPreDecision,
                                 ) {
    val payoutPrices = MPrice.toSumPricesByCurrency( decision.dealWith )
    val payoutPrice = payoutPrices.head._2
    lazy val comissionDealPrices = MPrice.toSumPricesByCurrency( decision.outOfDeal )
    lazy val sioPayInPrice = comissionDealPrices.head._2
  }

}


/** JMX-интерфейс. */
trait Bill2UtilJmxMBean {

  def fsckNodesContracts(): String

}

class Bill2UtilJmx @Inject()(
                              injector: Injector,
                            )
  extends JmxBase
  with Bill2UtilJmxMBean
  with MacroLogsDyn
{

  import io.suggest.util.JmxBase._

  private def bill2Util = injector.instanceOf[Bill2Util]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  override def _jmxType = Types.BILL

  override def fsckNodesContracts(): String = {
    val logPrefix = s"fsckNodesContracts()#${System.currentTimeMillis()}:"
    LOGGER.info(s"$logPrefix Starting")
    val strFut = for {
      res <- bill2Util.fsckNodesContracts()
    } yield {
      val msg = s"Done => $res"
      LOGGER.info(s"$logPrefix $msg")
      msg
    }
    awaitString( strFut )
  }

}
