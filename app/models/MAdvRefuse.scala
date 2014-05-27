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
 * Created: 23.05.14 17:50
 * Description: Модель для хранения записей об отказах в размещении рекламы. Т.е. некий антипод [[MAdvOk]].
 */

object MAdvRefuse extends MAdvStatic[MAdvRefuse] {
  import SqlParser._

  override val TABLE_NAME = "adv_refuse"

  override val rowParser = {
    ROW_PARSER_BASE ~ get[DateTime]("date_status") ~ get[String]("reason") map {
      case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ comission ~ mode ~ onStartPage ~ dateStart ~ dateEnd ~ prodAdnId ~ rcvrAdnId ~ dateStatus ~ reason =>
        MAdvRefuse(
          id          = id,
          adId        = adId,
          amount      = amount,
          currencyCode = currencyCode,
          dateCreated = dateCreated,
          comission   = comission,
          onStartPage = onStartPage,
          dateStatus  = dateStatus,
          dateStart   = dateStart,
          dateEnd     = dateEnd,
          reason      = reason,
          rcvrAdnId   = rcvrAdnId,
          prodAdnId   = prodAdnId
        )
    }
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
  onStartPage   : Boolean,
  dateStart     : DateTime,
  dateEnd       : DateTime,
  dateStatus    : DateTime = DateTime.now,
  dateCreated   : DateTime = DateTime.now,
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvRefuse] with CurrencyCode with SqlModelDelete with MAdvI {

  override def mode = MAdvModes.REFUSED
  override def hasId: Boolean = id.isDefined
  override def companion = MAdvRefuse

  override def saveInsert(implicit c: Connection): MAdvRefuse = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission, mode, on_start_page, date_status, reason, prod_adn_id, rcvr_adn_id, date_start, date_end) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {comission}, {mode}, {onStartPage}, {dateStatus}, {reason}, {prodAdnId}, {rcvrAdnId}, {dateStart}, {dateEnd})")
    .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
        'comission -> comission, 'mode -> mode.toString, 'onStartPage -> onStartPage, 'dateStatus -> dateStatus,
        'reason -> reason, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId, 'dateStart -> dateStart, 'dateEnd -> dateEnd)
    .executeInsert(rowParser single)
  }

  /**
   * Обновления ряда в таблице не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0
}
