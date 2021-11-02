package io.suggest.mbill2.m.txn

import java.time.OffsetDateTime
import javax.inject.Inject
import io.suggest.mbill2.m.balance.{BalanceIdFkSlick, BalanceIdInxSlick, FindByBalanceId, MBalances}
import io.suggest.mbill2.m.common.InsertOneReturning
import io.suggest.mbill2.m.gid.{DeleteById, GetById, GidSlick, Gid_t}
import io.suggest.mbill2.m.item.{ItemIdOptFkSlick, ItemIdOptInxSlick, ItemIdOptSlick, MItems}
import io.suggest.mbill2.m.order.{MOrders, OrderIdOptFkSlick, OrderIdOptInxSlick, OrderIdOptSlick}
import io.suggest.mbill2.m.price.AmountSlick
import io.suggest.mbill2.util.PgaNamesMaker
import io.suggest.pay.{MPaySystem, MPaySystems}
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import play.api.inject.Injector
import japgolly.univeq._
import play.api.libs.json.JsValue
import slick.lifted.ProvenShape

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 17:08
 * Description: slick-модель транзакций на счетах.
 */
final class MTxns @Inject() (
                              injector: Injector,
                              override protected val profile    : SioPgSlickProfileT,
                            )
  extends GidSlick
  with AmountSlick
  with BalanceIdFkSlick with BalanceIdInxSlick
  with InsertOneReturning
  with GetById
  with OrderIdOptSlick with OrderIdOptFkSlick with OrderIdOptInxSlick
  with FindByBalanceId
  with ItemIdOptSlick with ItemIdOptFkSlick with ItemIdOptInxSlick
  with DeleteById
{

  override lazy val mBalances = injector.instanceOf[MBalances]
  override lazy val mOrders = injector.instanceOf[MOrders]
  override lazy val mItems = injector.instanceOf[MItems]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  import profile.api._


  override type Table_t = MTxnsTable
  override type El_t    = MTxn

  override def TABLE_NAME = "txn"

  def DATE_PAID_FN        = "date_paid"
  def DATE_PROCESSED_FN   = "date_processed"
  def PAYMENT_COMMENT_FN  = "comment"
  def PS_TXN_UID_FN       = "ps_txn_uid"
  def TX_TYPE_FN          = "txtype"

  override def BALANCE_ID_INX   = PgaNamesMaker.fkInx(TABLE_NAME, BALANCE_ID_FN)

  class MTxnsTable(tag: Tag)
    extends Table[MTxn](tag, TABLE_NAME)
    with GidColumn
    with BalanceIdColumn with BalanceIdInx
    with AmountColumn
    with OrderIdOpt with OrderIdOptFk with OrderIdOptInx
    with ItemIdOpt with ItemIdOptFk with ItemIdOptInx
  {

    def datePaidOpt     = column[Option[OffsetDateTime]](DATE_PAID_FN)
    def dateProcessed   = column[OffsetDateTime](DATE_PROCESSED_FN)
    def paymentComment  = column[Option[String]](PAYMENT_COMMENT_FN)

    def psTxnUidOpt     = column[Option[String]](PS_TXN_UID_FN)
    def psTxnUidKey     = index(PgaNamesMaker.uniq(TABLE_NAME, PS_TXN_UID_FN), psTxnUidOpt, unique = true)

    def txTypeStr       = column[String](TX_TYPE_FN)
    def txType          = txTypeStr <> (MTxnTypes.withValue, MTxnType.unapplyStrId)

    def metadata        = column[Option[JsValue]]( MTxn.Fields.METADATA )

    def paySystemStr    = column[Option[String]]( MTxn.Fields.PAY_SYSTEM )
    def paySystem       = paySystemStr <> (
      _.map(MPaySystems.withValue),
      {paySystemOpt: Option[MPaySystem] =>
        Some( paySystemOpt.map(_.value) )
      },
    )

    override def * : ProvenShape[MTxn] = {
      (balanceId, amount, txType, orderIdOpt, itemIdOpt, paymentComment, paySystem, psTxnUidOpt, datePaidOpt, dateProcessed, metadata, id.?) <> (
        (MTxn.apply _).tupled, MTxn.unapply
      )
    }

  }

  override protected def _withId(el: MTxn, id: Gid_t): MTxn = {
    el.copy(id = Some(id))
  }

  override lazy val query = TableQuery[MTxnsTable]

  /** Обычно идёт постраничный просмотр списка транзакций, и новые сверху.
    * Тут метод для сборки подходящего для этого запроса.
    *
    * @param balanceIds Множество id балансов юзера.
    * @param limit Макс.кол-во возвращаемых результатов.
    * @param offset Абсолютный сдвиг в результатах.
    * @return DBIOAction со списком транзакций.
    */
  def findLatestTxns(balanceIds: Iterable[Gid_t] = Nil, limit: Int, offset: Int = 0) = {
    findByBalanceIdsBuilder(balanceIds)
      .drop(offset)
      .take(limit)
      .sortBy(_.id.desc)
      .result
  }

  /** For multi-step payments, this DBIO-action can be used for closing not-yet-closed pending transaction. */
  def updatePaidInfo( txn: MTxn ): DBIOAction[MTxn, NoStream, Effect.Write] = {
    assert( txn.datePaid.nonEmpty, s"datePaid must be non-empty: $txn" )
    for {
      countUpdated <- query
        .filter( _.id === txn.id.get )
        .map { tx =>
          (tx.datePaidOpt, tx.paymentComment, tx.txType)
        }
        .update(( txn.datePaid, txn.paymentComment, txn.txType ))
      if countUpdated ==* 1
    } yield {
      txn
    }
  }


  /** Mark transaction as cancelled without any checks. */
  def updateTypeAndComment(txnId: Gid_t, newTxType: MTxnType, comment: Option[String], when: OffsetDateTime = OffsetDateTime.now()): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .filter( _.id === txnId )
      .map { t =>
        (t.txType, t.paymentComment, t.dateProcessed)
      }
      .update((newTxType, comment, when))
  }

  def saveTypeAndComment(mtxn: MTxn): DBIOAction[Int, NoStream, Effect.Write] =
    updateTypeAndComment( mtxn.id.get, mtxn.txType, mtxn.paymentComment, mtxn.dateProcessed )

}

