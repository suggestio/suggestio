package io.suggest.mbill2.m.txn

import com.google.inject.{Inject, Singleton}
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.balance.{FindByBalanceId, MBalances, BalanceIdInxSlick, BalanceIdFkSlick}
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.gid.{GetById, Gid_t, GidSlick}
import io.suggest.mbill2.m.order.{MOrders, OrderIdOptInxSlick, OrderIdOptFkSlick, OrderIdOptSlick}
import io.suggest.mbill2.m.price.{Amount_t, AmountSlick}
import io.suggest.mbill2.util.PgaNamesMaker
import org.joda.time.DateTime
import slick.lifted.ProvenShape

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 17:08
 * Description: slick-модель транзакций на счетах.
 */

@Singleton
class MTxns @Inject() (
  override protected val driver     : ExPgSlickDriverT,
  override val mBalances            : MBalances,
  override val mOrders              : MOrders
)
  extends GidSlick
  with AmountSlick
  with BalanceIdFkSlick with BalanceIdInxSlick
  with InsertOneReturning
  with GetById
  with OrderIdOptSlick with OrderIdOptFkSlick with OrderIdOptInxSlick
  with FindByBalanceId
{

  import driver.api._

  override type Table_t = MTxnsTable
  override type El_t    = MTxn

  override val TABLE_NAME = "txn"

  def DATE_PAID_FN        = "date_paid"
  def DATE_PROCESSED_FN   = "date_processed"
  def PAYMENT_COMMENT_FN  = "comment"
  def PS_TXN_UID_FN       = "ps_txn_uid"

  override def BALANCE_ID_INX   = PgaNamesMaker.fkInx(TABLE_NAME, BALANCE_ID_FN)

  class MTxnsTable(tag: Tag)
    extends Table[MTxn](tag, TABLE_NAME)
    with GidColumn
    with BalanceIdColumn with BalanceIdInx
    with AmountColumn
    with OrderIdOpt with OrderIdOptFk with OrderIdOptInx
  {

    def datePaidOpt     = column[Option[DateTime]](DATE_PAID_FN)
    def dateProcessed   = column[DateTime](DATE_PROCESSED_FN)
    def paymentComment  = column[Option[String]](PAYMENT_COMMENT_FN)

    def psTxnUidOpt     = column[Option[String]](PS_TXN_UID_FN)
    def psTxnUidKey     = index(PgaNamesMaker.uniq(TABLE_NAME, PS_TXN_UID_FN), psTxnUidOpt, unique = true)

    override def * : ProvenShape[MTxn] = {
      (balanceId, amount, orderIdOpt, paymentComment, psTxnUidOpt, datePaidOpt, dateProcessed, id.?) <> (
        MTxn.tupled, MTxn.unapply
      )
    }

  }

  override protected def _withId(el: MTxn, id: Gid_t): MTxn = {
    el.copy(id = Some(id))
  }

  override val query = TableQuery[MTxnsTable]

  /** Обычно идёт постраничный просмотр списка транзакций, и новые сверху.
    * Тут метод для сборки подходящего для этого запроса.
    *
    * @param balanceIds Множество id балансов юзера.
    * @param limit Макс.кол-во возвращаемых результатов.
    * @param offset Абсолютный сдвиг в результатах.
    * @return DBIOAction со списком транзакций.
    */
  def findLatestTxns(balanceIds: Traversable[Gid_t], limit: Int, offset: Int) = {
    findByBalanceIdsBuilder(balanceIds)
      .drop(offset)
      .take(limit)
      .sortBy(_.id.desc)
      .result
  }

}


case class MTxn(
  balanceId         : Gid_t,
  amount            : Amount_t,
  orderIdOpt        : Option[Gid_t],
  paymentComment    : Option[String]      = None,
  psTxnUidOpt       : Option[String]      = None,
  datePaid          : Option[DateTime]    = None,
  dateProcessed     : DateTime            = DateTime.now(),
  id                : Option[Gid_t]       = None
)
