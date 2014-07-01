package models

import anorm._
import util.AnormJodaTime._
import util.AnormPgArray._
import org.joda.time.DateTime
import java.sql.Connection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 11:43
 * Description: Журнал транзакций по биллингу.
 */
object MBillTxn extends SqlModelStatic {
  import SqlParser._

  override type T = MBillTxn

  val TABLE_NAME: String = "bill_txn"

  val rowParser = get[Option[Int]]("id") ~ get[Int]("contract_id") ~ get[Float]("amount") ~
                  get[Option[String]]("currency") ~ get[DateTime]("date_paid") ~ get[DateTime]("date_processed") ~
                  get[String]("payment_comment") ~ get[String]("txn_uid") ~ get[Option[String]]("ad_id") ~
                  get[Option[Float]]("comission_pc") map {
    case id ~ contractId ~ amount ~ currencyCodeOpt ~ datePaid ~ dateProcessed ~ paymentComment ~ txnUid ~ adId ~ comissionPc =>
      MBillTxn(
        id = id,  contractId = contractId,  amount = amount,  currencyCodeOpt = currencyCodeOpt,
        datePaid = datePaid,  dateProcessed = dateProcessed,  paymentComment = paymentComment,
        txnUid = txnUid, adId = adId, comissionPc = comissionPc
      )
  }

  /**
   * Найти все транзакции для указанного контракта.
   * @param contractId id контракта.
   * @return Список транзакций, новые сверху.
   */
  def findForContract(contractId: Int)(implicit c: Connection): List[MBillTxn] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE contract_id = {contractId} ORDER BY id DESC")
      .on('contractId -> contractId)
      .as(rowParser *)
  }

  /**
   * Найти все транзакции для указанного списка id контрактов (списка номеров договоров).
   * @param contractIds Список номеров договоров.
   * @return Список транзакций в порядке их появления.
   */
  def findForContracts(contractIds: Seq[Int])(implicit c: Connection): List[MBillTxn] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE contract_id = ANY({ids}) ORDER BY id ASC")
      .on('ids -> seqInt2pgArray(contractIds))
      .as(rowParser *)
  }

  /**
   * Прочитать n последних платежей, т.е. с положительным amount.
   * @param count Размер выдачи.
   * @return
   */
  def lastNPayments(count: Int = 10)(implicit c: Connection): List[MBillTxn] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE amount > 0 ORDER BY id DESC LIMIT {count}")
      .on('count -> count)
      .as(rowParser *)
  }

}


import MBillTxn._

case class MBillTxn(
  contractId      : Int,
  amount          : Float,
  datePaid        : DateTime,
  txnUid          : String,
  currencyCodeOpt : Option[String] = None,
  dateProcessed   : DateTime = DateTime.now(),
  paymentComment  : String,
  adId            : Option[String] = None,
  comissionPc     : Option[Float] = None,
  id              : Option[Int] = None
) extends CurrencyCodeOpt {

  /**
   * Добавить в базу транзакцию.
   * @return Новый экземпляр сабжа.
   */
  def save(implicit c: Connection): MBillTxn = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, amount, currency, date_paid, date_processed, payment_comment, txn_uid, ad_id, comission_pc)" +
        " VALUES({contractId}, {amount}, {currencyCode}, {datePaid}, {dateProcessed}, {paymentComment}, {txnUid}, {adId}, {comissionPc})")
      .on('contractId -> contractId, 'amount -> amount, 'currencyCode -> currencyCodeOpt,
          'datePaid -> datePaid, 'dateProcessed -> dateProcessed, 'paymentComment -> paymentComment,
          'txnUid -> txnUid, 'adId -> adId, 'comissionPc -> comissionPc)
      .executeInsert(rowParser single)
  }

}
