package models

import anorm._
import MAdv._
import org.joda.time.DateTime
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
    ROW_PARSER_BASE ~ get[DateTime]("date_status") ~ get[Int]("prod_txn_id") ~ get[Option[Int]]("rcvr_txn_id") map {
      case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ comission ~ mode ~ onStartPage ~ dateStart ~ dateEnd ~ prodAdnId ~ rcvrAdnId ~ dateStatus ~ prodTxnId ~ rcvrTxnId =>
        MAdvOk(
          id          = id,
          adId        = adId,
          amount      = amount,
          currencyCode = currencyCode,
          dateCreated = dateCreated,
          comission   = comission,
          dateStatus  = dateStatus,
          dateStart   = dateStart,
          dateEnd     = dateEnd,
          onStartPage = onStartPage,
          prodTxnId   = prodTxnId,
          rcvrTxnId   = rcvrTxnId,
          prodAdnId   = prodAdnId,
          rcvrAdnId   = rcvrAdnId
        )
    }
  }

}


import MAdvOk._


case class MAdvOk(
  adId          : String,
  amount        : Float,
  currencyCode  : String = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  comission     : Option[Float],
  dateStart     : DateTime,
  dateEnd       : DateTime,
  prodTxnId     : Int,
  rcvrTxnId     : Option[Int],
  onStartPage   : Boolean,
  prodAdnId     : String,
  rcvrAdnId     : String,
  dateCreated   : DateTime = DateTime.now(),
  dateStatus    : DateTime = DateTime.now(),
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvOk] with CurrencyCode with SiowebSqlModel[MAdvOk] with MAdvI {

  override def mode = MAdvModes.OK
  override def hasId: Boolean = id.isDefined
  override def companion = MAdvOk

  override def saveInsert(implicit c: Connection): MAdvOk = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission, mode, on_start_page, date_start, date_end, prod_adn_id, rcvr_adn_id, date_status, prod_txn_id, rcvr_txn_id) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {comissionPc}, {mode}, {onStartPage}, {dateStart}, {dateEnd}, {prodAdnId}, {rcvrAdnId}, {dateStatus}, {prodTxnId}, {rcvrTxnId})")
    .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
        'comission -> comission, 'dateStart -> dateStart, 'mode -> mode.toString, 'onStartPage -> onStartPage,
        'dateStatus -> dateStatus, 'dateStart -> dateStart, 'dateEnd -> dateEnd, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId,
        'dateStatus -> dateStatus, 'prodTxnId -> prodTxnId, 'rcvrTxnId -> rcvrTxnId)
    .executeInsert(rowParser single)
  }

  /**
   * Обновление (редактирование) записей не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0
}
