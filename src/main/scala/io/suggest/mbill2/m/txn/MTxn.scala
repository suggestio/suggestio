package io.suggest.mbill2.m.txn

import com.google.inject.{Inject, Singleton}
import io.suggest.common.slick.driver.ExPgSlickDriverT
import io.suggest.mbill2.m.balance.{MBalances, BalanceIdInxSlick, BalanceIdFkSlick}
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.gid.{GetById, Gid_t, GidSlick}
import io.suggest.mbill2.m.item._
import io.suggest.mbill2.m.price.{Amount_t, AmountSlick}
import io.suggest.mbill2.m.txn.comis._
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
  override val mItems               : MItems
)
  extends GidSlick
  with AmountSlick
  with BalanceIdFkSlick with BalanceIdInxSlick
  with InsertOneReturning
  with GetById
  with ComissionOptSlick with ComissionBalanceIdOptFkSlick with ComissionBalanceIdOptInxSlick
  with ItemIdOptSlick with ItemIdOptFkSlick with ItemIdOptInxSlick
{

  import driver.api._

  override type Table_t = MTxnsTable
  override type El_t    = MTxn

  override val TABLE_NAME = "txn"

  def DATE_PAID_FN        = "date_paid"
  def DATE_PROCESSED_FN   = "date_processed"
  def PAYMENT_COMMENT_FN  = "payment_comment"
  def PS_TXN_UID_FN       = "ps_txn_uid"

  override def BALANCE_ID_INX   = PgaNamesMaker.fkInx(TABLE_NAME, BALANCE_ID_FN)

  class MTxnsTable(tag: Tag)
    extends Table[MTxn](tag, TABLE_NAME)
    with GidColumn
    with BalanceIdColumn with BalanceIdInx
    with AmountColumn
    with ComissionOpt with ComissionBalanceIdOptFk with ComissionBalanceIdOptInx
    with ItemIdOpt with ItemIdOptFk with ItemIdOptInx
  {

    def datePaidOpt     = column[Option[DateTime]](DATE_PAID_FN)
    def dateProcessed   = column[DateTime](DATE_PROCESSED_FN)
    def paymentComment  = column[Option[String]](PAYMENT_COMMENT_FN)

    def psTxnUidOpt     = column[Option[String]](PS_TXN_UID_FN)
    def psTxnUidKey     = index(PgaNamesMaker.uniq(TABLE_NAME, PS_TXN_UID_FN), psTxnUidOpt)

    override def * : ProvenShape[MTxn] = {
      (balanceId, amount, itemIdOpt, paymentComment, psTxnUidOpt, comissionOpt, datePaidOpt, dateProcessed, id.?) <> (
        MTxn.tupled, MTxn.unapply
      )
    }

  }

  override protected def _withId(el: MTxn, id: Gid_t): MTxn = {
    el.copy(id = Some(id))
  }

  override val query = TableQuery[MTxnsTable]

}


case class MTxn(
  balanceId         : Gid_t,
  amount            : Amount_t,
  itemIdOpt         : Option[Gid_t],
  paymentComment    : Option[String],
  psTxnUidOpt       : Option[String],
  comissionOpt      : Option[MComission]  = None,
  datePaid          : Option[DateTime]    = None,
  dateProcessed     : DateTime            = DateTime.now(),
  id                : Option[Gid_t]       = None
)
