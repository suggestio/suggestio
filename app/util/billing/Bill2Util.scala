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
import io.suggest.mbill2.m.txn.{MTxns, MTxn}
import models.adv.tpl.MAdvPricing
import models.mbill.MCartIdeas
import models.mproj.ICommonDi
import models.{IPrice, CurrencyCodeOpt, MNode, MPrice}
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

  // Короткие алиасы для типов составных эффектов DBIOAction
  type RW   = Effect.Read with Effect.Write
  type RWT  = RW with Effect.Transactional

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
  def getCartOrder(contractId: Gid_t): Future[Option[MOrder]] = {
    dbConfig.db.run {
      mOrders.getCartOrder(contractId)
    }
  }

  /**
    * Убедиться, что для контракта существует ордер-корзина для покупок.
    *
    * @param contractId Номер договора.
    * @return Фьючерс с ордером корзины.
    */
  def ensureCart(contractId: Gid_t): Future[MOrder] = {
    // Возможно, надо объединить поиск ордера и создания в одну транзакцию, хз...
    val ocOptFut = getCartOrder(contractId)
    ocOptFut
      .map(_.get)
      .recoverWith { case ex: NoSuchElementException =>
        val cartOrderStub = MOrder(MOrderStatuses.Draft, contractId)
        val fut = dbConfig.db.run {
          mOrders.insertOne(cartOrderStub)
        }
        fut.onSuccess { case cartOrder =>
          LOGGER.debug(s"ensureNodeCart($contractId): Initialized new cart order[${cartOrder.id}]")
        }
        fut
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
      mitems        <- mItems.findByOrderIdAction(orderId)
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
          mItems.saveStatus(itm2)
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
  def maybeExecuteOrder(order: IOrderWithItems): DBIOAction[MCartIdeas.Idea, NoStream, RW] = {

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
      // Запустить поиск узла CBCA, чтобы на него зачислять деньги на баланс.
      val cbcaNodeOptFut = mNodeCache.getById( CBCA_NODE_ID )

      // Товары есть, и они не бесплатны.
      LOGGER.trace(logPrefix + "There are priceful items in cart.")

      for {
        // Узнаём текущее финансовое состояние юзера...
        balances     <- mBalances.findByContractId( order.morder.contractId ).forUpdate

        // Собрать карту балансов CBCA, чтобы туда зачислять бабло, сграбленное у юзера.
        cbcaNodeOpt  <- DBIO.from(cbcaNodeOptFut)
        cbcaBalances <- getCbcaBalances( cbcaNodeOpt )
        // id контракта CBCA для зачисления денег:
        cbcaContractIdOpt = cbcaNodeOpt.flatMap(_.billing.contractId)

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
                  LOGGER.trace(s"$logPrefix Skip building balance action, because of notEnounghtAcc.nonEmpty")
                  acc0

                } else {
                  // Баблишка на балансе хватает для оплаты покупок в текущей валюте.
                  // Собрать DB-экшен обновления баланса и закинуть в аккамулятор:
                  val mbalance2 = balance0.copy(
                    price = balance0.price.copy(
                      amount = balAmount2
                    )
                  )
                  LOGGER.trace(s"$logPrefix Enought money on balance[${balance0.id.orNull}] for totalPrice=$totalPrice. New balance = ${mbalance2.price}")
                  val dbAction = for {
                    // Записать новый баланс юзера
                    rowsUpdated <- mBalances.updateAmount(mbalance2)
                    if rowsUpdated == 1

                    // Добавить транзакцию списания денег с баланса юзера
                    usrTxn <- {
                      val utxn = MTxn(
                        balanceId   = mbalance2.id.get,
                        amount      = -totalPrice.amount,
                        orderIdOpt  = order.morder.id
                      )
                      mTxns.insertOne(utxn)
                    }

                    // Залить средства на баланс CBCA, чтобы деньги не списывались с баланса юзера безвозвратно.
                    _ <- cbcaContractIdOpt.fold [DBIOAction[_, NoStream, Effect.Write]] {
                      DBIO.successful(0)
                    } { cbcaContractId =>
                      increaseBalance(order.morder.id, cbcaContractId, cbcaBalances, totalPrice)
                    }

                  } yield {
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
    lazy val logPrefix = s"increaseCbcaBalance($orderIdOpt->$rcvrContractId,$price):"
    // Залить средства на баланс CBCA.
    val cbcaBalanceAction = balancesMap
      .get( price.currencyCode )
      .fold [DBIOAction[MBalance, NoStream, Effect.Write]] {
        LOGGER.trace(logPrefix + "Initializing new CBCA balance for this currency...")
        val cb0 = MBalance(rcvrContractId, price)
        mBalances.insertOne(cb0)

      } { cb0 =>
        LOGGER.trace(logPrefix + "Increasing CBCA balance...")
        val cb1 = cb0.copy(
          price = cb0.price.copy(
            amount = cb0.price.amount + price.amount
          )
        )
        for {
          rowsUpdated <- mBalances.updateAmount(cb1)
          if rowsUpdated == 1
        } yield {
          cb1
        }
      }

    for {
      cbcaBalance2  <- cbcaBalanceAction
      // Записать транзакцию по зачислению на баланс CBCA
      ctxn2         <- {
        val ctxn0 = MTxn(
          balanceId  = cbcaBalance2.id.get,
          amount     = price.amount,
          orderIdOpt = orderIdOpt
        )
        mTxns.insertOne(ctxn0)
      }
    } yield {
      ctxn2
    }
  }


  /**
    * При оплате с балансов юзеров списываются деньги, но списываются они на счёт CBCA.
    *
    * @param cbcaNodeOpt Узел CBCA, если есть.
    * @return DB-экшен, возвращающий карту балансов.
    */
  def getCbcaBalances(cbcaNodeOpt: Option[MNode]): DBIOAction[Map[String,MBalance], NoStream, Effect.Read] = {
    def logMsg(msg: String) = "getCbcaBalances(): " + msg + " Money income will be lost."

    cbcaNodeOpt
      .flatMap(_.billing.contractId)
      // map + getOrElse вместо fold чтобы не писать тип возвращаемого значения второй раз.
      .map { contractId =>
        for {
          balances     <- mBalances.findByContractId( contractId ).forUpdate
        } yield {
          mBalances.balances2curMap(balances)
        }
      }
      // Should never happen, отработка когда нет CBCA-ноды.
      .getOrElse {
        LOGGER.warn( logMsg(s"CBCA node[$CBCA_NODE_ID] OR it's contractId is missing: $cbcaNodeOpt") )
        DBIO.successful {
          Map.empty
        }
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
      txnRes  <- maybeExecuteOrder(cart0)
    } yield {
      // Сформировать результат работы экшена
      txnRes
    }

    // Форсировать весь этот экшен в транзакции:
    dbAction.transactionally
  }

}


/** Интерфейс для DI. */
trait IBill2UtilDi {
  /** Инстанс DI-поля. */
  def bill2Util: Bill2Util
}
