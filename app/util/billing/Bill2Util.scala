package util.billing

import java.util.Currency

import com.google.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.order.{MOrderStatuses, MOrders, MOrder}
import models.adv.tpl.MAdvPricing
import models.{CurrencyCodeOpt, MNode, MPrice}
import org.elasticsearch.client.Client
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import util.PlayMacroLogsImpl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 13:49
 * Description: Утиль для биллинга второго поколения, с ордерами и корзинами.
 */
@Singleton
class Bill2Util @Inject() (
  dbConfigProvider                : DatabaseConfigProvider,
  mOrders                         : MOrders,
  mContracts                      : MContracts,
  implicit private val ec         : ExecutionContext,
  implicit private val esClient   : Client,
  implicit private val sn         : SioNotifierStaticClientI
)
  extends PlayMacroLogsImpl
{

  private val dbConfig = dbConfigProvider.get[JdbcProfile]


  /**
   * Найти и вернуть контракт для указанного id.
   * Если контракт не найден или не сущесвует, то он будет создан.
   * @param mnode Узел.
   * @return Фьючерс с экземпляром MContract.
   */
  def ensureNodeContract(mnode: MNode): Future[MContract] = {
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

    // Отработать случай, когда контракта нет.
    mcOptFut
      .map(_.get)
      .recoverWith { case _: NoSuchElementException =>

        // Контракт не найден, значит нужно создать новый и вернуть.
        val mc = MContract()
        val resFut = dbConfig.db.run {
          mContracts.insertOne(mc)
        }

        // Сохранить id свежесозданного контракта в текущую ноду
        resFut.flatMap { mc2 =>

          // Запустить обновление текущего узла.
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
              LOGGER.trace(s"$logPrefix Initialized new contract[${mc2.id}] for node.")
            // Не удалось сохранить contract_id в ноду, откатить свежесозданный ордер
            case Failure(ex) =>
              for (id <- mc2.id) {
                dbConfig.db.run {
                  mOrders.deleteById(id)
                }
              }
              LOGGER.error(s"$logPrefix Rollback contact[${mc2.id}] init, because unable to update MNode.")
          }

          // Вернуть созданный контракт
          updFut.flatMap { _ =>
            resFut
          }
        }
      }
  }


  /**
   * Убедиться, что для контракта существует ордер-корзина для покупок.
   * @param contractId Номер договора.
   * @return Фьючерс с ордером корзины.
   */
  def ensureNodeCart(contractId: Gid_t): Future[MOrder] = {
    val ocOptFut = dbConfig.db.run {
      mOrders.getCartOrder(contractId)
    }
    ocOptFut
      .map(_.get)
      .recoverWith { case ex: NoSuchElementException =>
        val cartOrderStub = MOrder(MOrderStatuses.Draft, contractId, zeroPrice)
        val fut = dbConfig.db.run {
          mOrders.insertOne(cartOrderStub)
        }
        fut.onSuccess { case cartOrder =>
          LOGGER.trace(s"ensureNodeCart($contractId): Initialized new cart order[${cartOrder.id}]")
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

}
