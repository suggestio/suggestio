package io.suggest.mbill2.m.balance

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.{Amount_t, IMCurrency, MCurrency, MPrice}
import io.suggest.common.m.sql.ITableName
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.contract.{ContractIdSlickFk, ContractIdSlickIdx, FindByContractId, MContracts}
import io.suggest.mbill2.m.gid._
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.util.PgaNamesMaker
import slick.lifted._
import slick.sql.SqlAction

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:13
 * Description: Модель остатка на счетах по договору.
 */

@Singleton
class MBalances @Inject() (
  override protected val profile      : SioPgSlickProfileT,
  override protected val mContracts   : MContracts,
  implicit private val ec             : ExecutionContext
)
  extends GidSlick
  with PriceSlick
  with AmountSlick
  with CurrencyCodeSlick
  with ContractIdSlickFk with ContractIdSlickIdx
  with ITableName
  with GetById
  with MultiGetById
  with InsertOneReturning
  with FindByContractId
{

  import profile.api._

  override type Table_t = MBalancesTable
  override type El_t    = MBalance

  override val TABLE_NAME = "balance"

  override def CONTRACT_ID_INX = PgaNamesMaker.inx(TABLE_NAME, CONTRACT_ID_FN)
  def CCC_UNIQUE_IDX = PgaNamesMaker.uniq(TABLE_NAME, CONTRACT_ID_FN, CURRENCY_CODE_FN)

  def BLOCKED_FN = "blocked"
  def LOW_FN     = "low"

  /** slick-описание таблицы остатков на счетах. */
  class MBalancesTable(tag: Tag)
    extends Table[El_t](tag, TABLE_NAME)
    with GidColumn
    with PriceColumn with CurrencyColumn
    with AmountColumn
    with CurrencyCodeColumn
    with ContractIdFk with ContractIdIdx
  {

    def blocked = column[Amount_t](BLOCKED_FN)
    def lowOpt  = column[Option[Amount_t]](LOW_FN)

    def currencyContractInx = index(CCC_UNIQUE_IDX, (contractId, currencyCode), unique = true)

    /** default projection (SELECT *). */
    override def * : ProvenShape[MBalance] = {
      (contractId, price, blocked, lowOpt, id.?) <> (
        MBalance.tupled, MBalance.unapply
      )
    }

  }

  override protected def _withId(el: El_t, id: Gid_t): El_t = {
    el.copy(id = Some(id))
  }

  override val query = TableQuery[MBalancesTable]

  /** Атомарный инкремент баланса и декремент blocked по id ряда.
    *
    * @return Новый баланс и иновый blocked. None если ряд не найден.
    */
  def incrAmountAndBlockedBy(id: Gid_t, delta: Amount_t): DBIOAction[Option[(Amount_t, Amount_t)], NoStream, Effect.Write] = {
    sql"UPDATE #$TABLE_NAME SET #$BLOCKED_FN = #$BLOCKED_FN - $delta, #$AMOUNT_FN = #$AMOUNT_FN + $delta WHERE #$ID_FN = $id RETURNING #$AMOUNT_FN, #$BLOCKED_FN"
      .as[(Amount_t, Amount_t)]
      .map { _.headOption }
  }
  /** Атомарное обновления колонок баланса и blocked.
    * amount будет увеличена на delta, а blocked -- уменьшена на delta.
    *
    * @param balance0 Исходный инстанс баланса.
    * @param delta На сколько единиц валюты увеличить баланс и уменьшить blocked?
    * @return Some([[MBalance]]) с обновлёнными балансами.
    *         Или None, если баланс не найден в таблице.
    */
  def incrAmountAndBlockedBy(balance0: MBalance, delta: Amount_t): DBIOAction[Option[MBalance], NoStream, Effect.Write] = {
    for (resOpt <- incrAmountAndBlockedBy(balance0.id.get, delta)) yield {
      for ( (amount2, blocked2) <- resOpt ) yield {
        balance0.copy(
          blocked = blocked2,
          price   = balance0.price.withAmount( amount2 )
        )
      }
    }
  }

  /** Изменить значение blocked для указанного баланса. */
  def incrBlockedBy(id: Gid_t, delta: Amount_t) = _incrColBy(id, BLOCKED_FN, delta)

  /** Изменить значение amount для указанного баланса. */
  def incrAmountBy(id: Gid_t, delta: Amount_t) = _incrColBy(id, AMOUNT_FN, delta)

  private def _incrColBy(id: Gid_t, fn: String, delta: Amount_t): DBIOAction[Option[Amount_t], NoStream, Effect.Write] = {
    sql"UPDATE #$TABLE_NAME SET #$fn = #$fn + $delta WHERE #$ID_FN = $id RETURNING #$fn"
      .as[Amount_t]
      .map { _.headOption }
  }

  /** Атомарное обновление баланса.
    * amount будет увеличена на delta.
    *
    * @param balance0 Исходный баланс.
    * @param delta На сколько увеличить amount?
    * @return Some([[MBalance]]) если всё ок.
    *         None, если в таблице нет указанного баланса.
    */
  def incrAmountBy(balance0: MBalance, delta: Amount_t): DBIOAction[Option[MBalance], NoStream, Effect.Write] = {
    for (resOpt <- incrAmountBy(balance0.id.get, delta)) yield {
      for (amount2 <- resOpt) yield {
        balance0.withPrice(
          balance0.price.withAmount( amount2 )
        )
      }
    }
  }


  /** Жестко перезаписать объемы средств на балансе.
    *
    * @param bal Обновлённый баланс.
    * @return db-экшен, возвращающий кол-во обновлённых рядов.
    */
  def saveAmountAndBlocked(bal: MBalance): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .filter(_.id === bal.id.get)
      .map { b =>
        (b.amount, b.blocked)
      }
      .update((bal.price.amount, bal.blocked))
  }


  /**
    * Поиск по id контракта и валюте.
    * Эта комбинация является уникальным ключом в рамках таблицы, поэтому максимум один результат.
    *
    * @param contractId id контракта.
    * @param currencies Искомые валюты.
    * @return Опциональных [[MBalance]].
    */
  def getByContractCurrency(contractId: Gid_t, currencies: MCurrency*): SqlAction[Option[MBalance], NoStream, Effect.Read] = {
    getByContractCurrency1(contractId, currencies)
  }
  def getByContractCurrency1(contractId: Gid_t, currencies: Traversable[MCurrency]): SqlAction[Option[MBalance], NoStream, Effect.Read] = {
    assert(currencies.nonEmpty)
    query
      .filter { b =>
        (b.contractId === contractId) && (b.currencyCode inSet currencies.map(_.currencyCode))
      }
      .result
      .headOption
  }

  /**
    * Инициализировать пустой кошелек для указанной валюты и контракта.
    *
    * @param contractId id контракта.
    * @param currency валюта кошелька.
    * @return DBIOAction, возвращающий экземпляр созданного кошелька.
    */
  def initByContractCurrency(contractId: Gid_t, currency: MCurrency): DBIOAction[MBalance, NoStream, Effect.Write] = {
    val mb = MBalance(
      contractId  = contractId,
      price       = MPrice(0.0, currency)
    )
    insertOne(mb)
  }

}


/** Экземпляр модели. */
case class MBalance(
  contractId        : Gid_t,
  price             : MPrice,
  blocked           : Amount_t          = 0.0,
  lowOpt            : Option[Amount_t]  = None,
  override val id   : Option[Gid_t]     = None
)
  extends IGid
  with IMCurrency
{

  def low = lowOpt.getOrElse( 0.0 )

  def withPrice(price2: MPrice) = copy(price = price2)

  override def currency = price.currency

  def withPriceBlocked(price2: MPrice, blocked2: Amount_t = blocked) = copy(
    price   = price2,
    blocked = blocked2
  )

  def plusAmount(amount: Amount_t) = withPriceBlocked(
    price2 = price.plusAmount(amount)
  )

  def unblockAmount(amount2: Amount_t) = withPriceBlocked(
    price2   = price.plusAmount(amount2),
    blocked2 = blocked - amount2
  )

  def blockAmount(amount2: Amount_t) = unblockAmount(-amount2)

  override def toString: String = {
    s"${getClass.getSimpleName}(#${id.orNull},c$contractId,$price+$blocked${lowOpt.fold("")(",low=" + _)})"
  }

}
