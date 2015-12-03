package io.suggest.mbill2.m.txn

import com.google.inject.{Inject, Singleton}
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.balance.{MBalances, BalanceIdInxSlick, BalanceIdFkSlick}
import io.suggest.mbill2.m.gid.{Gid_t, GidSlick}
import io.suggest.mbill2.m.order.MOrders
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
  protected val mOrders             : MOrders,
  override protected val mBalances  : MBalances
)
  extends GidSlick
  with AmountSlick
  with BalanceIdFkSlick with BalanceIdInxSlick
{

  import driver.api._

  override val TABLE_NAME = "txn"

  def DATE_PAID_FN        = "date_paid"
  def DATE_PROCESSED_FN   = "date_processed"
  def PAYMENT_COMMENT_FN  = "payment_comment"
  def PS_TXN_UID_FN       = "ps_txn_uid"
  def ORDER_ID_FN         = "order_id"

  override def BALANCE_ID_INX   = PgaNamesMaker.fkInx(TABLE_NAME, BALANCE_ID_FN)

  class MTxnsTable(tag: Tag)
    extends Table[MTxn](tag, TABLE_NAME)
    with GidColumn
    with BalanceIdColumn with BalanceIdInx
    with AmountColumn
  {

    def datePaidOpt     = column[Option[DateTime]](DATE_PAID_FN)
    def dateProcessed   = column[DateTime](DATE_PROCESSED_FN)
    def paymentComment  = column[Option[String]](PAYMENT_COMMENT_FN)
    def psTxnUidOpt     = column[Option[String]](PS_TXN_UID_FN)
    def orderIdOpt      = column[Option[Gid_t]](ORDER_ID_FN)

    def order           = foreignKey( PgaNamesMaker.fkey(TABLE_NAME, ORDER_ID_FN), orderIdOpt, mOrders.orders)(_.id.?)

    def orderInx        = index(PgaNamesMaker.fkInx(TABLE_NAME, ORDER_ID_FN), orderIdOpt)
    def psTxnUidKey     = index(PgaNamesMaker.uniq(TABLE_NAME, PS_TXN_UID_FN), psTxnUidOpt)

    override def * : ProvenShape[MTxn] = {
      (balanceId, amount, orderIdOpt, paymentComment, psTxnUidOpt, datePaidOpt, dateProcessed, id.?) <> (
        MTxn.tupled, MTxn.unapply
      )
    }

  }

}


case class MTxn(
  balanceId         : Gid_t,
  amount            : Amount_t,
  orderIdOpt        : Option[Gid_t],
  paymentComment    : Option[String],
  psTxnUidOpt       : Option[String],
  datePaid          : Option[DateTime]  = None,
  dateProcessed     : DateTime          = DateTime.now(),
  id                : Option[Gid_t]     = None
)
