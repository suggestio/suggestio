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
 * Created: 23.05.14 17:50
 * Description: Модель для хранения записей об отказах в размещении рекламы. Т.е. некий антипод [[MAdvOk]].
 */

object MAdvRefuse extends MAdvStatic {
  import SqlParser._

  override type T = MAdvRefuse

  override val TABLE_NAME = "adv_refuse"

  override val rowParser = {
    ADV_ROW_PARSER_1 ~ get[DateTime]("date_status") ~ get[String]("reason") ~ ADV_ROW_PARSER_2 map {
      case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ comission ~ mode ~ dateStart ~ dateEnd ~ prodAdnId ~
        rcvrAdnId ~ dateStatus ~ reason ~ showLevels =>
        MAdvRefuse(
          id          = id,
          adId        = adId,
          amount      = amount,
          currencyCode = currencyCode,
          dateCreated = dateCreated,
          comission   = comission,
          dateStatus  = dateStatus,
          dateStart   = dateStart,
          dateEnd     = dateEnd,
          reason      = reason,
          rcvrAdnId   = rcvrAdnId,
          prodAdnId   = prodAdnId,
          showLevels  = showLevels
        )
    }
  }


  def apply(adv: MAdvI, reason: String, dateStatus1: DateTime): MAdvRefuse = {
    import adv._
    MAdvRefuse(
      id          = id,
      adId        = adId,
      amount      = amount,
      currencyCode = currencyCode,
      dateCreated = dateCreated,
      comission   = comission,
      dateStatus  = dateStatus1,
      dateStart   = dateStart,
      dateEnd     = dateEnd,
      reason      = reason,
      rcvrAdnId   = rcvrAdnId,
      prodAdnId   = prodAdnId,
      showLevels  = showLevels
    )
  }
}


import MAdvRefuse._


case class MAdvRefuse(
  adId          : String,
  amount        : Float,
  currencyCode  : String,
  comission     : Option[Float],
  reason        : String,
  prodAdnId     : String,
  rcvrAdnId     : String,
  dateStart     : DateTime,
  dateEnd       : DateTime,
  showLevels    : Set[SinkShowLevel],
  dateStatus    : DateTime = DateTime.now,
  dateCreated   : DateTime = DateTime.now,
  id            : Option[Int] = None
) extends SqlModelSave[MAdvRefuse] with SqlModelDelete with MAdvI {

  override def mode = MAdvModes.REFUSED
  override def hasId: Boolean = id.isDefined
  override def companion = MAdvRefuse

  override def saveInsert(implicit c: Connection): MAdvRefuse = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission, mode, show_levels, date_status, reason, prod_adn_id, rcvr_adn_id, date_start, date_end) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {comission}, {mode}, {showLevels}, {dateStatus}, {reason}, {prodAdnId}, {rcvrAdnId}, {dateStart}, {dateEnd})")
    .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
        'comission -> comission, 'mode -> mode.toString, 'showLevels -> strings2pgArray(showLevels), 'dateStatus -> dateStatus,
        'reason -> reason, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId, 'dateStart -> dateStart, 'dateEnd -> dateEnd)
    .executeInsert(rowParser single)
  }

  /**
   * Обновления ряда в таблице не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0
}
