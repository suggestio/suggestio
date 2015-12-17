package io.suggest.mbill2.m.balance

import com.google.inject.{Inject, Singleton}
import io.suggest.common.m.sql.ITableName
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.contract.{FindByContractId, MContracts, ContractIdSlickIdx, ContractIdSlickFk}
import io.suggest.mbill2.m.gid.{GetById, Gid_t, GidSlick}
import io.suggest.mbill2.m.price._
import io.suggest.mbill2.util.PgaNamesMaker
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 10:13
 * Description: Модель остатка на счетах по договору.
 */

@Singleton
class MBalances @Inject() (
  override protected val driver       : ExPgSlickDriverT,
  override protected val mContracts   : MContracts
)
  extends GidSlick
  with PriceSlick
  with AmountSlick
  with CurrencyCodeSlick
  with ContractIdSlickFk with ContractIdSlickIdx
  with ITableName
  with GetById
  with InsertOneReturning
  with FindByContractId
{

  import driver.api._

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

}


/** Экземпляр модели. */
case class MBalance(
  contractId  : Gid_t,
  price       : MPrice,
  blocked     : Amount_t,
  lowOpt      : Option[Amount_t],
  id          : Option[Gid_t]
) {

  def low = lowOpt getOrElse 0.0

}
