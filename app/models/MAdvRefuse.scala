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
 * Created: 23.05.14 17:50
 * Description: Модель для хранения записей об отказах в размещении рекламы. Т.е. некий антипод [[MAdvOk]].
 */

object MAdvRefuse extends SiowebSqlModelStatic[MAdvRefuse] {
  import SqlParser._

  override val TABLE_NAME = "adv_refuse"

  override val rowParser = {
    ROW_PARSER_BASE ~ get[DateTime]("date_refused") ~ get[String]("reason") ~ get[String]("refuser_adn_id") ~ get[String]("prod_adn_id") map {
      case id ~ adId ~ amount ~ currencyCodeOpt ~ dateCreated ~ comissionPc ~ period ~ dateRefused ~ reason ~ refuserAdnId ~ prodAdnId =>
        MAdvRefuse(
          id          = id,
          adId        = adId,
          amount      = amount,
          currencyCodeOpt = currencyCodeOpt,
          dateCreated = dateCreated,
          comissionPc = comissionPc,
          period      = period,
          dateRefused = dateRefused,
          reason      = reason,
          refuserAdnId = refuserAdnId,
          prodAdnId   = prodAdnId
        )
    }
  }
}


import MAdvRefuse._


case class MAdvRefuse(
  adId          : String,
  amount        : Float,
  currencyCodeOpt: Option[String] = None,
  comissionPc   : Option[Float],
  period        : Period,
  reason        : String,
  refuserAdnId  : String,
  prodAdnId     : String,
  dateRefused   : DateTime = DateTime.now,
  dateCreated   : DateTime = DateTime.now,
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvRefuse] with CurrencyCodeOpt with SiowebSqlModel[MAdvRefuse] with MAdvI {

  override def hasId: Boolean = id.isDefined
  override def companion = MAdvRefuse

  override def saveInsert(implicit c: Connection): MAdvRefuse = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission_pc, period, date_refused, reason, refuser_adn_id, prod_adn_id) " +
      "VALUES ({adId}, {amount}, {currencyCodeOpt}, {dateCreated}, {comissionPc}, {period}, {dateRefused}, {reason}, {refuserAdnId}, {prodAdnId})")
    .on('adId -> adId, 'amount -> amount, 'currencyCodeOpt -> currencyCodeOpt, 'dateCreated -> dateCreated,
        'comissionPc -> comissionPc, 'period -> period, 'dateRefused -> dateRefused, 'reason -> reason,
        'refuserAdnId -> refuserAdnId, 'prodAdnId -> prodAdnId)
    .executeInsert(rowParser single)
  }

  /**
   * Обновления ряда в таблице не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0
}
