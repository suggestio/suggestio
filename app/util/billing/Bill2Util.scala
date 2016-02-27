package util.billing

import java.util.Currency

import com.google.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{IItem, MItems, MItem}
import io.suggest.mbill2.m.order._
import io.suggest.mbill2.m.txn.{MTxnTypes, MTxns, MTxn}
import io.suggest.mbill2.util.effect._
import models.adv.tpl.MAdvPricing
import models.mbill.MCartIdeas
import models.mproj.ICommonDi
import models.{IPrice, CurrencyCodeOpt, MNode, MPrice}
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import play.api.Play.isProd

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 13:49
 * Description: Утиль для биллинга второго поколения, с ордерами и корзинами.
 */
@Singleton
class Bill2Util @Inject() (
  mOrders                         : MOrders,
  mContracts                      : MContracts,
  mItems                          : MItems,
  mBalances                       : MBalances,
  mTxns                           : MTxns,
  val mCommonDi                   : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import dbConfig.driver.api._

  /** id узла, на который должна сыпаться комиссия с этого биллинга. */
  val CBCA_NODE_ID: String = {
    configuration
      .getString("bill.cbca.node.id")
      .getOrElse {
        if (isProd) {
          // узел cbca в кластере sio2prod.
          "-vr-hrgNRd6noyQ3_teu_A"
        } else {
          // test-узел какой-то в кластере sio2dev.
          "AUzledEIITehtyXq7GtI"
        }
      }
  }

  def cbcaNodeOptFut = mNodeCache.getById(CBCA_NODE_ID)

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
      val fut = dbConfig.db.run {
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
          dbConfig.db.run {
            mContracts.insertOne(mc)
          }
        }

        // Сохранить id свежесозданного контракта в текущую ноду
        mnode2 <- {
          val updFut = MNode.tryUpdate(mnode) { mnode0 =>
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
                dbConfig.db.run {
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

  /** Найти ордер-корзину. */
  def getCartOrder(contractId: Gid_t): DBIOAction[Option[MOrder], NoStream, Effect.Read] = {
    mOrders.getCartOrder(contractId)
  }

  /**
    * Убедиться, что для контракта существует ордер-корзина для покупок.
    *
    * @param contractId Номер договора.
    * @return Фьючерс с ордером корзины.
    */
  def ensureCart(contractId: Gid_t): DBIOAction[MOrder, NoStream, RW] = {
    getCartOrder(contractId).flatMap {
      case None =>
        val cartOrderStub = MOrder(MOrderStatuses.Draft, contractId)
        val orderAct = mOrders.insertOne(cartOrderStub)
        orderAct.map { order2 =>
          LOGGER.debug(s"ensureNodeCart($contractId): Initialized new cart order[${order2.id.orNull}]")
          order2
        }
      case Some(order) =>
        DBIO.successful(order)
    }
  }

  /** Нулевая цена. */
  def zeroPrice: MPrice = {
    val curr = Currency.getInstance(CurrencyCodeOpt.CURRENCY_CODE_DFLT)
    MPrice(0.0, curr)
  }

  /** Нулевой прайсинг размещения. */
  def zeroPricing: MAdvPricing = {
    val prices = Seq(zeroPrice)
    MAdvPricing(prices, hasEnoughtMoney = true)
  }

  /** Найти все item'ы указанного ордера. */
  def orderItems(orderId: Gid_t): Future[Seq[MItem]] = {
    dbConfig.db.run {
      mItems.findByOrderId(orderId)
    }
  }


  /** Подготовится к транзакции внутри корзины. */
  def prepareCartTxn(contractId: Gid_t): DBIOAction[MOrderWithItems, NoStream, Effect.Read] = {
    for {
      cartOrderOpt  <- mOrders.getCartOrder(contractId).forUpdate
      order         = cartOrderOpt.get
      orderId       = order.id.get
      mitems        <- mItems.findByOrderIdBuilder(orderId)
        .filter(_.statusStr === MItemStatuses.Draft.strId)
        .result
        .forUpdate
    } yield {
      LOGGER.trace(s"prepareCartTxn($contractId): cart order[$orderId] contains ${mitems.size} items.")
      MOrderWithItems(order, mitems)
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
      .groupBy(_.currency.getCurrencyCode)
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
  def hasPositivePrice(prices: TraversableOnce[IPrice]): Boolean = {
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

    lazy val logPrefix = s"processOrder(${order.morder.id.orNull}/${order.mitems.size}items;${System.currentTimeMillis}):"

    if (order.mitems.isEmpty) {
      // Корзина пуста. Should not happen.
      LOGGER.trace(logPrefix + " no items in the cart")
      DBIO.successful( MCartIdeas.NothingToDo )

    } else if ( !itemsHasPrice(order.mitems) ) {
      // Итемы есть, но всё бесплатно. Исполнить весь этот контракт прямо тут.
      LOGGER.trace(logPrefix + " Only FREE items in cart.")
      for (_ <- closeOrder(order)) yield {
        MCartIdeas.OrderClosed(order, Nil)
      }

    } else {

      // Товары есть, и они не бесплатны.
      LOGGER.trace(logPrefix + "There are priceful items in cart.")

      for {
        // Узнаём текущее финансовое состояние юзера...
        balances     <- mBalances.findByContractId( order.morder.contractId ).forUpdate

        /*
        // Запустить поиск узла CBCA, чтобы на него зачислять деньги на баланс.
        val cbcaNodeOptFut = mNodeCache.getById( CBCA_NODE_ID )

        // Собрать карту балансов CBCA, чтобы туда зачислять бабло, сграбленное у юзера.
        cbcaNodeOpt  <- DBIO.from(cbcaNodeOptFut)
        cbcaBalances <- getCbcaBalances( cbcaNodeOpt )
        // id контракта CBCA для зачисления денег:
        cbcaContractIdOpt = cbcaNodeOpt.flatMap(_.billing.contractId)

        // Залить средства на баланс CBCA, чтобы деньги не списывались с баланса юзера безвозвратно.
        _ <- cbcaContractIdOpt.fold [DBIOAction[_, NoStream, Effect.Write]] {
          DBIO.successful(0)
        } { cbcaContractId =>
          increaseBalance(order.morder.id, cbcaContractId, cbcaBalances, totalPrice)
        }
        */

        // Строим карту полученных балансов юзера для удобства работы:
        balancesMap = mBalances.balances2curMap(balances)

        // Считаем полную стоимость заказа-корзины.
        totalPrices = items2pricesIter(order.mitems).toSeq

        // Пройтись по ценам, узнать достаточно ли бабла у юзера на балансах по всем валютам заказа
        (balUpdActions, notEnoughtPrices) = {
          val balActionsAcc = List.empty[DBIOAction[MBalance, NoStream, Effect.Write]]
          val notEnougtAcc = List.empty[MPrice]

          totalPrices.foldLeft ((balActionsAcc, notEnougtAcc)) {
            case (acc0 @ (enought0, notEnough0), totalPrice) =>
              import totalPrice.currencyCode
              balancesMap.get( currencyCode ).fold {
                // Пока ещё нет такой валюты на балансах юзера. Юзеру нужно оплатить всю стоимость в валюте.
                LOGGER.trace(s"$logPrefix Balance for currency $currencyCode is missing. Guessing as zero amount.")
                (enought0, totalPrice :: notEnough0)

              } { balance0 =>
                val balAmount2 = balance0.price.amount - totalPrice.amount
                if (balAmount2 < balance0.low) {
                  // Баблишко-то вроде есть, но его не хватает. Юзеру нужно доплатить разность.
                  val needAmount = -balAmount2
                  LOGGER.trace(s"$logPrefix User needs to pay some money: $needAmount $currencyCode")
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
              MCartIdeas.NeedMoney(notEnoughtPrices)
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
      balancesMap =  mBalances.balances2curMap(balances)
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
  def increaseBalance(orderIdOpt: Option[Gid_t], rcvrContractId: Gid_t, balancesMap: Map[String, MBalance],
                      price: MPrice): DBIOAction[MTxn, NoStream, Effect.Write] = {
    lazy val logPrefix = s"increaseBalance($orderIdOpt->$rcvrContractId,$price): "
    // Залить средства на баланс CBCA.
    val cbcaBalanceAction = balancesMap
      .get( price.currencyCode )
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
          .getByIdStatusAction(itemId, MItemStatuses.AwaitingSioAuto)
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
          .getByContractCurrency(contractId, mitem.price.currencyCode)
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
      rcvrNodeOpt <- mNodeCache.getById( nodeId )
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
  sealed case class ApproveItemResult(mitem: MItem)

  /**
    * Аппрув модерация item'а.
    * Происходит списание заблокированных денег из кошелька юзера, зачисление средств в пользу CBCA,
    * обновление item'a.
    *
    * @param itemId id обрабатываемого item'а.
    * @return Экшен с результатом работы.
    */
  def approveItemAction(itemId: Gid_t): DBIOAction[ApproveItemResult, NoStream, RW] = {
    lazy val logPrefix = s"approveItemAction($itemId):"

    for {
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
          prepareMoneyReceiver(mrNodeId)
        }
      }

      // Получить и заблокировать баланс покупателя
      balance0 <- _item2balance(mitem0)

      // Списать blocked amount с баланса покупателя
      usrAmtBlocked2 <- mBalances.incrBlockedBy(balance0.id.get, -mitem0.price.amount)

      // Запустить обновление полностью на стороне БД.
      mitem2 <- {
        val mi2 = mitem0.copy(
          status = MItemStatuses.Offline,
          dateStatus = DateTime.now()
        )
        mItems.query
          .filter(_.id === itemId)
          .map { i =>
            (i.status, i.dateStatus)
          }
          .update((mi2.status, mi2.dateStatus))
          .filter(_ == 1)
          .map(_ => mi2)
      }

      // Залить деньги получателю денег, если возможно.
      mrInfoOpt <- DBIO.from( mrInfoOptFut )
      _ <- {
        mrInfoOpt.fold [DBIOAction[_,_,RW]] {
          LOGGER.warn(s"$logPrefix Money income skipped, so they are lost.")
          DBIO.successful(None)

        } { mrInfo =>
          // Есть получатель финансов, зачислить ему на необходимый баланс.
          val price = mitem2.price
          for {
            // Найти/создать кошелек получателя денег
            mrBalance0 <- ensureBalanceFor(mrInfo.mc.id.get, price.currency)
            // Зачислить деньги как заблокированные, ведь клиент может быть не доволен оказанной услугой.
            mrBlockedAmount2Opt <- mBalances.incrBlockedBy(mrBalance0.id.get, price.amount)
            // Транзакцию не проводить, т.к. транзакция идёт по текущему счёту, а не по заблокированным средствам,
            // которые могут быть списаны назад при проблемах...
          } yield {
            val mrBlockedAmount2 = mrBlockedAmount2Opt.get
            LOGGER.debug(s"$logPrefix Money receiver balance[${mrBalance0.id.orNull}] blocked updated: ${mrBalance0.blocked} + ${price.amount} => $mrBlockedAmount2 ${mrBalance0.price.currencyCode}")
            mrBalance0.copy(
              blocked = mrBlockedAmount2
            )
          }
        }
      }

    } yield {
      ApproveItemResult( mitem2 )
    }
  }


  /**
    * Найти/создать баланс для указанного контракта и валюты.
    *
    * @param contractId id контракта.
    * @param currency Валюта баланса.
    * @return Экшен, возвращающий баланс, готовый к обновлению.
    */
  def ensureBalanceFor(contractId: Gid_t, currency: Currency): DBIOAction[MBalance, NoStream, RW] = {
    val currencyCode = currency.getCurrencyCode
    lazy val logPrefix = s"ensureBalanceFor($contractId,$currencyCode):"
    for {
      // Считать баланс получателя...
      balanceOpt <- {
        mBalances.getByContractCurrency(contractId, currencyCode)
          .forUpdate
      }

      // Если баланс отсутствует, то инициализировать
      balance0 <- {
        balanceOpt.fold [DBIOAction[MBalance, NoStream, RW]] {
          LOGGER.trace(s"$logPrefix Initializing money receiver's balance for $contractId/${}...")
          mBalances.initByContractCurrency(contractId, currency)
        } { DBIO.successful }
      }

    } yield {
      balance0
    }
  }


  /** Результат исполнения экшена refuseItemAction(). */
  sealed case class RefuseItemResult(mitem: MItem, mtxn: MTxn)

  /**
    * Item не прошел модерацию или продавец/поставщик отказал по какой-то [уважительной] причине.
    * Необходимо не забывать заворачивать в транзакцию весь этот код.
    *
    * @param itemId id итема, от которого отказывается продавец.
    * @param reasonOpt причина отказа, если есть.
    * @return
    */
  def refuseItemAction(itemId: Gid_t, reasonOpt: Option[String]): DBIOAction[RefuseItemResult, NoStream, RW] = {
    for {
      // Получить и заблокировать текущий item.
      mitem0 <- _prepareAwaitingItem(itemId)

      // Получить и заблокировать баланс юзера
      balance0 <- _item2balance(mitem0)

      // Разблокировать на балансе сумму с этого item'а
      amount2 <- mBalances.incrAmountAndBlockedBy(balance0, mitem0.price.amount)

      // Отметить item как отказанный в размещении
      mitem2 <- {
        // Чтобы вернуть новый item, не считывая его из таблицы повторно, имитируем его прямо тут...
        val mi2 = mitem0.copy(
          status      = MItemStatuses.Refused,
          dateStatus  = DateTime.now(),
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
        val mtxn0 = MTxn(
          balanceId  = balance0.id.get,
          amount     = mitem0.price.amount,
          txType     = MTxnTypes.Rollback,
          itemId     = Some(itemId)
        )
        mTxns.insertOne(mtxn0)
      }

    } yield {
      // Собрать итог работы экшена...
      RefuseItemResult(mitem2, mtxn2)
    }
  }

}


/** Интерфейс для DI. */
trait IBill2UtilDi {
  /** Инстанс DI-поля. */
  def bill2Util: Bill2Util
}
