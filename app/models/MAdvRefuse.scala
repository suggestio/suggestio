package models

import anorm._
import MAdv._
import org.joda.time.DateTime
import util.anorm.{AnormPgArray, AnormJodaTime}
import AnormJodaTime._
import AnormPgArray._
import util.SqlModelSave
import java.sql.Connection
import util.event.SiowebNotifier.Implicts.sn

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
    ADV_ROW_PARSER_1 ~ get[DateTime]("date_status") ~ get[String]("reason") ~ SHOW_LEVELS_PARSER map {
      case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ mode ~ dateStart ~ dateEnd ~ prodAdnId ~
        rcvrAdnId ~ dateStatus ~ reason ~ showLevels =>
        MAdvRefuse(
          id          = id,
          adId        = adId,
          amount      = amount,
          currencyCode = currencyCode,
          dateCreated = dateCreated,
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


final case class MAdvRefuse(
  adId          : String,
  amount        : Float,
  currencyCode  : String,
  reason        : String,
  prodAdnId     : String,
  rcvrAdnId     : String,
  dateStart     : DateTime,
  dateEnd       : DateTime,
  showLevels    : Set[SinkShowLevel],
  dateStatus    : DateTime = DateTime.now,
  dateCreated   : DateTime = DateTime.now,
  id            : Option[Int] = None
) extends MAdvRefuseT with SqlModelDelete with MAdvModelSave {

  override def mode = MAdvModes.REFUSED
  override def hasId: Boolean = id.isDefined
  override def companion = MAdvRefuse
}

/** mixin из-за использования abstract override для saveInsert. */
sealed trait MAdvRefuseT extends MAdvI with SqlModelSave {
  override type T = MAdvRefuse
  def reason: String

  def saveInsert(implicit c: Connection): T = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, mode, show_levels, date_status, reason, prod_adn_id, rcvr_adn_id, date_start, date_end) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {mode}, {showLevels}, {dateStatus}, {reason}, {prodAdnId}, {rcvrAdnId}, {dateStart}, {dateEnd})")
    .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
        'mode -> mode.toString, 'showLevels -> strings2pgArray(showLevels), 'dateStatus -> dateStatus,
        'reason -> reason, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId, 'dateStart -> dateStart, 'dateEnd -> dateEnd)
    .executeInsert(rowParser single)
  }
}
