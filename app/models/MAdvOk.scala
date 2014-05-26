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
object MAdvOk extends MAdvStatic[MAdvOk] {
  import SqlParser._

  val TABLE_NAME = "adv_ok"

  override val rowParser = {
    ROW_PARSER_BASE ~ get[DateTime]("date_ok") ~ get[Option[DateTime]]("date_start") ~
      get[Int]("prod_txn_id") ~ get[Option[Int]]("rcvr_txn_id") map {
      case id ~ adId ~ amount ~ currencyCodeOpt ~ dateCreated ~ comissionPc ~ period ~ mode ~ dateOk ~ dateStart ~ prodTxnId ~ rcvrTxnId =>
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

  /** Парсер для выхлопов [[findByAdIdWithRcvrAdnId()]]. */
  val rowWithAdnIdParser = get[String]("adnId") ~ rowParser map {
    case adnId ~ advOk  =>  adnId -> advOk
  }

  /**
   * Тоже самое, что и [[findByAdId()]], но добавляет колонку rcvr adnId для быстрого получения инфы о целевом узле.
   * Операция довольно жирная, активно использует INNER JOIN.
   * @param adId id рекламной карточки.
   * @return Список пар (adnId, [[MAdvOk]]) в неопределённом порядке.
   */
  def findByAdIdWithRcvrAdnId(adId: String)(implicit c: Connection): List[(String, MAdvOk)] = {
    SQL(s"SELECT mbc.adn_id, ao.* FROM $TABLE_NAME ao, ${MBillTxn.TABLE_NAME} mbt, ${MBillContract.TABLE_NAME} mbc " +
      "WHERE ao.ad_id = {adId} AND ao.rcvr_txn_id = mbt.id AND mbt.contract_id = mbc.id")
      .on('adId -> adId)
      .as(rowWithAdnIdParser *)
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

  override def mode = MAdvModes.OK
  override def hasId: Boolean = id.isDefined
  override def companion = MAdvOk

  override def saveInsert(implicit c: Connection): MAdvOk = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission_pc, period, mode, date_ok, date_start, prod_txn_id, rcvr_txn_id) " +
      "VALUES ({adId}, {amount}, {currencyCodeOpt}, {dateCreated}, {comissionPc}, {period}, {mode}, {dateOk}, {dateStart}, {prodTxnId}, {rcvrTxnId})")
    .on('adId -> adId, 'amount -> amount, 'currencyCodeOpt -> currencyCodeOpt, 'dateCreated -> dateCreated,
        'comissionPc -> comissionPc, 'period -> period, 'mode -> mode.toString, 'dateOk -> dateOk,
        'dateStart -> dateStart, 'prodTxnId -> prodTxnId, 'rcvrTxnId -> rcvrTxnId)
    .executeInsert(rowParser single)
  }

  /**
   * Обновление (редактирование) записей не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0
}
