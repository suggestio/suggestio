package models

import anorm._
import MAdv._
import org.joda.time.DateTime
import util.AnormJodaTime._
import util.AnormPgArray._
import util.SqlModelSave
import java.sql.Connection
import AdShowLevels.sls2strings

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
    ADV_ROW_PARSER_1 ~ get[DateTime]("date_status") ~ get[Option[Int]]("prod_txn_id") ~
      get[Option[Int]]("rcvr_txn_id") ~ get[Boolean]("online") ~ get[Boolean]("is_auto") ~ ADV_ROW_PARSER_2 ~
      get[Boolean]("is_partner") map {
      case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ comission ~ mode ~ dateStart ~ dateEnd ~
        prodAdnId ~ rcvrAdnId ~ dateStatus ~ prodTxnId ~ rcvrTxnId ~ isOnline ~ isAuto ~ showLevels ~ isPartner =>
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
          prodTxnId   = prodTxnId,
          rcvrTxnId   = rcvrTxnId,
          prodAdnId   = prodAdnId,
          rcvrAdnId   = rcvrAdnId,
          isOnline    = isOnline,
          isAuto      = isAuto,
          isPartner   = isPartner,
          showLevels  = showLevels
        )
    }
  }

  def apply(madv: MAdvI, comission1: Option[Float], dateStatus1: DateTime, prodTxnId: Option[Int],
            rcvrTxnId: Option[Int], isOnline: Boolean, isAuto: Boolean, isPartner: Boolean): MAdvOk = {
    import madv._
    MAdvOk(
      id          = id,
      adId        = adId,
      amount      = amount,
      currencyCode = currencyCode,
      dateCreated = dateCreated,
      comission   = comission1,
      dateStatus  = dateStatus1,
      dateStart   = dateStart,
      dateEnd     = dateEnd,
      prodTxnId   = prodTxnId,
      rcvrTxnId   = rcvrTxnId,
      prodAdnId   = prodAdnId,
      rcvrAdnId   = rcvrAdnId,
      isOnline    = isOnline,
      isAuto      = isAuto,
      isPartner   = isPartner,
      showLevels  = showLevels
    )
  }


  /**
   * Найти все одобренные заявки размещения, у которых флаг в online НЕ выставлен, но судя по датам пора бы уже
   * выбросить в выдачу.
   * @return Список [[MAdvOk]] в неопределённом порядке.
   */
  def findAllOfflineOnTime(implicit c: Connection): List[MAdvOk] = {
    SQL("SELECT * FROM " + TABLE_NAME + " WHERE NOT online AND date_start <= now() AND date_end >= now()")
      .as(rowParser *)
  }

  /** Найти все ряды, у которых date_end уже в прошлом. Т.е. неактуальные ряды. */
  def findDateEndExpired(policy: SelectPolicy = SelectPolicies.NONE)(implicit c: Connection): List[MAdvOk] = {
    findBy(" WHERE date_end <= now() AND online", policy)
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
  prodTxnId     : Option[Int],
  rcvrTxnId     : Option[Int],
  prodAdnId     : String,
  rcvrAdnId     : String,
  isAuto        : Boolean,
  showLevels    : Set[AdShowLevel],
  dateCreated   : DateTime = DateTime.now(),
  dateStatus    : DateTime = DateTime.now(),
  isOnline      : Boolean = false,
  isPartner     : Boolean = false,
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvOk] with CurrencyCode with SqlModelDelete with MAdvI {

  override def mode = MAdvModes.OK
  override def hasId: Boolean = id.isDefined
  override def companion = MAdvOk

  override def saveInsert(implicit c: Connection): MAdvOk = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission, mode, date_start, date_end, prod_adn_id, rcvr_adn_id," +
      " date_status, prod_txn_id, rcvr_txn_id, online, is_auto, show_levels, is_partner) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {comission}, {mode}, {dateStart}, {dateEnd}, {prodAdnId}, {rcvrAdnId}," +
      " {dateStatus}, {prodTxnId}, {rcvrTxnId}, {isOnline}, {isAuto}, {showLevels}, {isPartner})")
    .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
        'comission -> comission, 'dateStart -> dateStart, 'mode -> mode.toString, 'showLevels -> strings2pgArray(showLevels),
        'dateStatus -> dateStatus, 'dateStart -> dateStart, 'dateEnd -> dateEnd, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId,
        'dateStatus -> dateStatus, 'prodTxnId -> prodTxnId, 'rcvrTxnId -> rcvrTxnId, 'isOnline -> isOnline, 'isAuto -> isAuto,
        'isPartner -> isPartner)
    .executeInsert(rowParser single)
  }

  /**
   * Можно обновлять некоторые поле.
   * @return Кол-во обновлённых рядов. Т.е. 1 или 0.
   */
  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET online = {isOnline}, date_start = {dateStart}, date_end = {dateEnd} WHERE id = {id}")
      .on('id -> id.get, 'isOnline -> isOnline, 'dateStart -> dateStart, 'dateEnd -> dateEnd)
      .executeUpdate()
  }
}
