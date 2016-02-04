package util.billing

import java.util.Currency

import com.google.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItems, MItem}
import io.suggest.mbill2.m.order.{MOrder, MOrderStatuses, MOrders}
import models.adv.tpl.MAdvPricing
import models.mproj.ICommonDi
import models.{CurrencyCodeOpt, MNode, MPrice}
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
  mCommonDi                       : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._

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


  sealed case class PrepareCartTxnRes(order: MOrder, mitems: Seq[MItem])

  /** Подготовится к транзакции внутри корзины. */
  def prepareCartTxn(contractId: Gid_t) = {
    for {
      cartOrderOpt  <- mOrders.getCartOrder(contractId)
      order         = cartOrderOpt.get
      orderId       = order.id.get
      mitems        <- mItems.findByOrderId(orderId)
    } yield {
      PrepareCartTxnRes(order, mitems)
    }
  }

}


/** Интерфейс для DI. */
trait IBill2UtilDi {
  /** Инстанс DI-поля. */
  def bill2Util: Bill2Util
}
