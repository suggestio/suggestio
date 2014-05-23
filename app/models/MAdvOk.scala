package models

import anorm._
import MAdv._
import org.joda.time.{Period, DateTime}
import util.AnormPgInterval._
import util.AnormJodaTime._
import util.SqlModelSave
import java.sql.Connection

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 17:35
 * Description: Одобренные заявки на размещение рекламы, т.е. проведённые сделки.
 */
object MAdvOk extends SiowebSqlModelStatic[MAdvOk] {
  import SqlParser._

  val TABLE_NAME = "adv_ok"

  override val rowParser = {
    ROW_PARSER_BASE ~ get[DateTime]("date_ok") ~ get[Option[DateTime]]("date_start") ~
      get[Int]("prod_txn_id") ~ get[Option[Int]]("rcvr_txn_id") map {
      case id ~ adId ~ amount ~ currencyCodeOpt ~ dateCreated ~ comissionPc ~ period ~ dateOk ~ dateStart ~ prodTxnId ~ rcvrTxnId =>
        MAdvOk(
          id          = id,
          adId        = adId,
          amount      = amount,
          currencyCodeOpt = currencyCodeOpt,
          dateCreated = dateCreated,
          comissionPc = comissionPc,
          period      = period,
          dateOk      = dateOk,
          dateStart   = dateStart,
          prodTxnId   = prodTxnId,
          rcvrTxnId   = rcvrTxnId
        )
    }
  }

}


import MAdvOk._


case class MAdvOk(
  adId          : String,
  amount        : Float,
  currencyCodeOpt: Option[String] = None,
  comissionPc   : Option[Float],
  period        : Period,
  dateStart     : Option[DateTime],
  prodTxnId     : Int,
  rcvrTxnId     : Option[Int],
  dateCreated   : DateTime = DateTime.now(),
  dateOk        : DateTime = DateTime.now(),
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvOk] with CurrencyCodeOpt with SiowebSqlModel[MAdvOk] with MAdvI {

  override def hasId: Boolean = id.isDefined
  override def companion = MAdvOk

  override def saveInsert(implicit c: Connection): MAdvOk = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission_pc, period, date_ok, date_start, prod_txn_id, rcvr_txn_id) " +
      "VALUES ({adId}, {amount}, {currencyCodeOpt}, {dateCreated}, {comissionPc}, {period}, {dateOk}, {dateStart}, {prodTxnId}, {rcvrTxnId})")
    .on('adId -> adId, 'amount -> amount, 'currencyCodeOpt -> currencyCodeOpt, 'dateCreated -> dateCreated,
        'comissionPc -> comissionPc, 'period -> period, 'dateOk -> dateOk, 'dateStart -> dateStart,
        'prodTxnId -> prodTxnId, 'rcvrTxnId -> rcvrTxnId)
    .executeInsert(rowParser single)
  }

  /**
   * Обновление (редактирование) записей не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0
}
