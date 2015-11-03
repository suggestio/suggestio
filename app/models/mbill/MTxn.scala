package models.mbill

import java.sql.Connection

import anorm._
import models.{CurrencyCodeOpt, SqlModelStatic}
import org.joda.time.DateTime
import util.anorm.AnormJodaTime._
import util.anorm.AnormPgArray._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 11:43
 * Description: Журнал транзакций по биллингу.
 */
object MTxn extends SqlModelStatic {
  import SqlParser._

  override type T = MTxn

  val TABLE_NAME: String = "bill_txn"

  val rowParser = get[Option[Int]]("id") ~ get[Int]("contract_id") ~ get[Float]("amount") ~
                  get[Option[String]]("currency") ~ get[DateTime]("date_paid") ~ get[DateTime]("date_processed") ~
                  get[String]("payment_comment") ~ get[String]("txn_uid") ~ get[Option[String]]("ad_id") ~
                  get[Option[Float]]("comission_pc") map {
    case id ~ contractId ~ amount ~ currencyCodeOpt ~ datePaid ~ dateProcessed ~ paymentComment ~ txnUid ~ adId ~ comissionPc =>
      MTxn(
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
  def findForContract(contractId: Int, limit: Int = 10, offset: Int = 0)(implicit c: Connection): List[MTxn] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE contract_id = {contractId} ORDER BY id DESC LIMIT {limit} OFFSET {offset}")
      .on('contractId -> contractId, 'limit -> limit, 'offset -> offset)
      .as(rowParser *)
  }

  /**
   * Найти все транзакции для указанного списка id контрактов (списка номеров договоров).
   * @param contractIds Список номеров договоров.
   * @return Список транзакций в порядке их появления.
   */
  def findForContracts(contractIds: Traversable[Int], limit: Int = 10, offset: Int = 0)(implicit c: Connection): List[MTxn] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE contract_id = ANY({ids}) ORDER BY id DESC LIMIT {limit} OFFSET {offset}")
      .on('ids -> seqInt2pgArray(contractIds), 'limit -> limit, 'offset -> offset)
      .as(rowParser *)
  }

  /**
   * Прочитать n последних платежей, т.е. с положительным amount.
   * @param count Размер выдачи.
   * @return
   */
  def lastNPayments(count: Int = 10)(implicit c: Connection): List[MTxn] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE amount > 0 ORDER BY id DESC LIMIT {count}")
      .on('count -> count)
      .as(rowParser *)
  }

}


import models.mbill.MTxn._

final case class MTxn(
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
  def save(implicit c: Connection): MTxn = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, amount, currency, date_paid, date_processed, payment_comment, txn_uid, ad_id, comission_pc)" +
        " VALUES({contractId}, {amount}, {currencyCode}, {datePaid}, {dateProcessed}, {paymentComment}, {txnUid}, {adId}, {comissionPc})")
      .on('contractId -> contractId, 'amount -> amount, 'currencyCode -> currencyCodeOpt,
          'datePaid -> datePaid, 'dateProcessed -> dateProcessed, 'paymentComment -> paymentComment,
          'txnUid -> txnUid, 'adId -> adId, 'comissionPc -> comissionPc)
      .executeInsert(rowParser single)
  }

}
