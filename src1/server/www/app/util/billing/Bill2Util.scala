package util.billing

import java.time.{Duration, OffsetDateTime}

import com.google.inject.{Inject, Singleton}
import io.suggest.bill._
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{IItem, IMItem, MItem, MItems}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.txn.{MTxn, MTxnTypes, MTxns}
import io.suggest.mbill2.util.effect._
import io.suggest.model.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import models.mbill.MCartIdeas
import models.mproj.ICommonDi
import models.MNode
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

  private def _getDaysCountFix(days0: Int) = {
    Math.max(1, days0) + 1
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
      .recoverWith { case ex: NoSuchElementException =>
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
            case Success(_) =>
              LOGGER.debug(s"$logPrefix Initialized new contract[${mc2.id}] for node.")
            // Не удалось сохранить contract_id в ноду, откатить свежесозданный ордер
            case Failure(ex) =>
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


  /** Поиск последнего ордера указанного контракта и с указанным статусом. */
  def getLastOrder(contractId: Gid_t, status: MOrderStatus): SqlAction[Option[MOrder], NoStream, Effect.Read] = {
    mOrders.query
      .filter { q =>
        (q.contractId === contractId) && (q.statusStr === status.strId)
      }
      .sortBy(_.id.desc.nullsLast)
      .take(1)
      .result
      .headOption
  }


  /** Попытаться удалить ордер, если есть id. */
  def maybeDeleteOrder(orderIdOpt: Option[Gid_t]): DBIOAction[Int, NoStream, Effect.Write] = {
    orderIdOpt.fold [DBIOAction[Int, NoStream, Effect.Write]] {
      LOGGER.trace("maybeClearCart(): orderId is empty, skipping")
      DBIO.successful(0)
    } { deleteOrder }
  }
  /** Удалить ордер вместе с item'ами. Такое можно сделать только если не было транзакций по ордеру/item'ам. */
  def deleteOrder(orderId: Gid_t): DBIOAction[Int, NoStream, Effect.Write] = {
    for {
      // Удаляем все item'ы в корзине разом.
      itemsDeleted  <- mItems.deleteByOrderId(orderId)
      // Удаляем ордер корзины, транзакций по ней быть не должно, а больше зависимостей и нет.
      ordersDeleted <- mOrders.deleteById(orderId)
    } yield {
      LOGGER.debug(s"clearCart($orderId) cartOrderId[$orderId] cleared $itemsDeleted items with $ordersDeleted order.")
      itemsDeleted
    }
  }

  /** Найти корзину и очистить её. */
  def clearCart(contractId: Gid_t): DBIOAction[Int, NoStream, RWT] = {
    val a = for {
      cartOrderOpt    <- getLastOrder(contractId, MOrderStatuses.Draft)
      cartOrderIdOpt  = cartOrderOpt.flatMap(_.id)
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
    val prices = Seq(zeroPrice)
    MGetPriceResp(prices)
  }
  def zeroPricingFut = Future.successful( zeroPricing )


  def getAdvPricing(prices: Seq[MPrice]): MGetPriceResp = {
    // Если есть разные валюты, то операция уже невозможна.
    if (prices.nonEmpty) {
      MGetPriceResp(prices)
    } else {
      zeroPricing
    }
  }


  /** Найти все item'ы указанного ордера. */
  def orderItems(orderId: Gid_t): Future[Seq[MItem]] = {
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
      cartOrderOpt  <- getLastOrder(contractId, MOrderStatuses.Draft).forUpdate
      order         = cartOrderOpt.get
      res           <- prepareCartOrderItems(order)
    } yield {
      res
    }
  }

  /**
    * Посчитать цены на item'ы.
    *
    * @param mitems item'мы.
    * @return Список цен по валютам.
    */
  def items2pricesIter(mitems: TraversableOnce[IItem]): Iterator[MPrice] = {
    mitems.toIterator
      .map { _.price }
      .toSeq
      .groupBy(_.currency)
      .valuesIterator
      .map { prices =>
        prices.head.copy(
          amount = prices.iterator.map(_.amount).sum
        )
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


  /** slick-экшен для запуска внутри транзакции для исполнения экшена.
    * Наполнение ордера уже должно быть прочитано, это также помогает защититься
    * от проведения ордера с параллельным добавлением итемов.
    *
    * @param order Обрабатываемый ордер вместе с наполнением.
    * @return DBIO-экшен, с какими-то данными или без них.
    */
  def closeOrder(order: IOrderWithItems): DBIOAction[MOrderWithItems, NoStream, Effect.Write] = {
    for {
      // Обновляем статус ордера.
      morder3 <- {
        val morder2 = order.morder.withStatus( MOrderStatuses.Closed )
        mOrders.saveStatus( morder2 )
          .filter { _ == 1 }
          .map { _ => morder2 }
      }

      // Обновляем item'ы ордера, возвращаем обновлённые инстансы.
      mitems3 <- {
        val actions = for (itm <- order.mitems) yield {
          val itm2 = itm.withStatus( itm.iType.orderClosedStatus )
          mItems.updateStatus(itm2)
            .filter { _ == 1 }
            .map { _ => itm2 }
        }
        DBIO.sequence(actions)
      }
    } yield {
      // Собрать возвращаемый результат экшена
      MOrderWithItems(morder3, mitems3)
    }
  }



  /**
    * slick-экшен для выполнения анализа открытого ордера (товарной корзины) и принятия дальнейшего решения.
    * Решения описаны в TxnResults.
    *
    * @param order Результат prepareCartTxn().
    */
  def maybeExecuteCart(order: IOrderWithItems): DBIOAction[MCartIdeas.Idea, NoStream, RW] = {

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
        balances     <- mBalances.findByContractId( order.morder.contractId ).forUpdate

        // Строим карту полученных балансов юзера для удобства работы:
        balancesMap = MCurrencies.hardMapByCurrency(balances)

        // Считаем полную стоимость заказа-корзины.
        totalPrices = items2pricesIter(order.mitems).toSeq

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
                    LOGGER.trace(s"$logPrefix Blocked $totalPrice, updated balance is $balAmount2.")
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
            DBIO.successful {
              MCartIdeas.NeedMoney(order, notEnoughtPrices)
            }

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

  def increaseBalanceSimple(contractId: Gid_t, price: MPrice): DBIOAction[MTxn, NoStream, RWT] = {
    val dba = for {
      // TODO Можно сразу находить баланс в нужной валюте или же создавать его...
      balances    <- mBalances.findByContractId(contractId)
      balancesMap =  MCurrencies.hardMapByCurrency(balances)
      txn         <- increaseBalance(
        orderIdOpt      = None,
        rcvrContractId  = contractId,
        balancesMap     = balancesMap,
        price           = price
      )
    } yield {
      LOGGER.trace(s"increaseBalanceSimple($contractId,$price): ok")
      txn
    }
    dba.transactionally
  }

  /** Логика заливки на баланс денег. Обычно используется для нужд валютных кошельков CBCA.
    *
    * @param orderIdOpt Связанный с транзакций ордер, если есть.
    * @param rcvrContractId id контракта получателя денег (обычно CBCA).
    * @param balancesMap Карта текущих балансов получателя денег.
    * @param price Общая цена пополнения в валюте.
    * @return DBIOAction создания/обновления кошелька узла.
    */
  def increaseBalance(orderIdOpt: Option[Gid_t], rcvrContractId: Gid_t, balancesMap: Map[MCurrency, MBalance],
                      price: MPrice): DBIOAction[MTxn, NoStream, Effect.Write] = {
    lazy val logPrefix = s"increaseBalance($orderIdOpt->$rcvrContractId,$price): "
    // Залить средства на баланс CBCA.
    val cbcaBalanceAction = balancesMap
      .get( price.currency )
      .fold [DBIOAction[MBalance, NoStream, Effect.Write]] {
        val cb0 = MBalance(rcvrContractId, price)
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

    for {
      cbcaBalance2  <- cbcaBalanceAction

      // Записать транзакцию по зачислению на баланс CBCA
      ctxn2         <- {
        val ctxn0 = MTxn(
          balanceId   = cbcaBalance2.id.get,
          amount      = price.amount,
          txType      = MTxnTypes.Income,
          orderIdOpt  = orderIdOpt
        )
        LOGGER.trace(logPrefix + "Inserting txn for balance #" + cbcaBalance2.id.orNull)
        mTxns.insertOne(ctxn0)
      }
    } yield {
      ctxn2
    }
  }


  /**
    * Обработать корзину, т.е. прочитать её и принять какое-то решение, вернув инфу о принятом решении.
    *
    * @param contractId id контракта, на котором висит корзина.
    * @return DB-action с транзакцией внутри.
    */
  def processCart(contractId: Gid_t): DBIOAction[MCartIdeas.Idea, NoStream, RWT] = {
    val dbAction = for {
      // Прочитать текущую корзину
      cart0   <- prepareCartTxn( contractId )
      // На основе наполнения корзины нужно выбрать дальнейший путь развития событий:
      txnRes  <- maybeExecuteCart(cart0)
    } yield {
      // Сформировать результат работы экшена
      txnRes
    }

    // Форсировать весь этот экшен в транзакции:
    dbAction.transactionally
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
  def approveItemAction(itemId: Gid_t): DBIOAction[ApproveItemResult, NoStream, RWT] = {
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
          status      = MItemStatuses.Offline,
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
      mrBalances2Opt <- {
        LOGGER.trace(s"$logPrefix item[${mitem2.id.orNull}] status: ${mitem0.status} => ${mitem2.status}")

        mrInfoOpt.fold [DBIOAction[Option[MBalance], NoStream, RW]] {
          LOGGER.warn(s"$logPrefix Money income skipped, so they are lost.")
          DBIO.successful(None)

        } { mrInfo =>
          // Есть получатель финансов, зачислить ему на необходимый баланс.
          val price = mitem2.price
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
    * Необходимо не забывать заворачивать в транзакцию весь этот код.
    *
    * @param itemId id итема, от которого отказывается продавец.
    * @param reasonOpt причина отказа, если есть.
    * @return
    */
  def refuseItemAction(itemId: Gid_t, reasonOpt: Option[String]): DBIOAction[RefuseItemResult, NoStream, RW] = {
    lazy val logPrefix = s"refuseItemAction($itemId):"
    for {
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
          // Есть какие-то деньги на валютном балансе юзера. Посчитать остаток.
          val needAmount = op.amount - curBal.price.amount
          if (needAmount > 0) {
            val op2 = op.withAmount(
              op.amount - curBal.price.amount
            )
            Some(op2)
          } else {
            None
          }
        }
      }
  }


  /**
    * Вычислить стоимости оплаты заказа в валютах заказа.
    *
    * @param orderId id заказа.
    * @param mBalancesFut Балансы из request.user.mBalancesFut
    * @return Фьючерс с ценниками по валютам на оплату.
    */
  def getPayPrices(orderId: Gid_t, mBalancesFut: Future[Seq[MBalance]]): Future[Seq[MPrice]] = {
     // Рассчитать стоимость текущих item'ов текущего заказа.
    val orderPricesFut = slick.db.run {
      getOrderPrices(orderId)
    }

    // Узнать, сколько остатков денег у юзера на балансах. Сгруппировать по валютам.
    val balsMapFut = for (ubs <- mBalancesFut) yield {
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

  /** Проверить и заморозить ордер для платежной системы.
    * Работы идут в виде транзакции, которая завершается исключением при любой проблеме.
    *
    * Метод дёргается на check-стадии оплаты: платежная система ещё не списала деньги,
    * но хочет проверить присланные юзером данные по оплачиваемому заказу.
    * S.io проверяет ордер и "холдит" его, чтобы защититься от изменений до окончания оплаты.
    *
    * @param orderId Заявленный платежной системой order_id. Ордер будет проверен на связь с контрактом.
    * @param validContractId Действительный contract_id юзера, выверенный по максимуму.
    * @param claimedOrderPrices Цены заказа по мнению платежной системы.
    *                           Map: валюта -> цена.
    * @return DB-экшен.
    */
  def checkOrder(orderId: Gid_t, validContractId: Gid_t,
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
        payPrices.forall { payPrice =>
          val claimedPrice = claimedOrderPrices( payPrice.currency )
          // Double сравнивать -- дело неблагодатное. Поэтому сравниваем в рамках погрешности:
          // 1 копейка или 0.01 рубля -- это предел, после которого цены не совпадают.
          val maxDiff = Math.pow(10, -payPrice.currency.exponent)
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


  /** Враппер вокруг maybeExecuteCart(), который выполняет действия для проведения платежа в платежной системе.
    *
    * @param orderId id ордера.
    * @param contractId id контракта.
    * @param mprice Объем платежа, проведённый в платежной системе.
    * @return RWT-db-экшен, возвращающий MCartIdea.
    */
  def handlePaySysPayment(orderId: Gid_t, contractId: Gid_t, mprice: MPrice): DBIOAction[MCartIdeas.Idea, NoStream, RWT] = {
    val a = for {
    // Проверить ордер, что он в статусе HOLD или DRAFT.
      mOrder0 <- getOpenedOrderForUpdate(orderId, validContractId = contractId)

      // Закинуть объем перечисленных денег на баланс юзера.
      _ <- increaseBalanceSimple(contractId, mprice)

      // Подготовить ордер корзины к исполнению.
      owi <- prepareCartOrderItems(mOrder0)

      // Запустить действия, связанные с вычитанием бабла с баланса юзера и реализацией MItem'ов заказа.
      cartIdea <- maybeExecuteCart(owi)

    } yield {
      cartIdea
    }
    a.transactionally
  }

}


/** Интерфейс для DI. */
trait IBill2UtilDi {
  /** Инстанс DI-поля. */
  def bill2Util: Bill2Util
}
