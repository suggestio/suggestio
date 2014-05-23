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
 * Created: 23.05.14 17:04
 * Description: Список запросов на размещение рекламы.
 */
object MAdvReq extends SiowebSqlModelStatic[MAdvReq] {
  import SqlParser._

  val TABLE_NAME = "adv_req"

  val rowParser = ROW_PARSER_BASE ~ get[Int]("prod_contract_id") ~ get[String]("rcvr_adn_id") map {
    case id ~ adId ~ amount ~ currencyCodeOpt ~ dateCreated ~ comissionPc ~ period ~ prodContractId ~ rcvrAdnId =>
      MAdvReq(
        id          = id,
        adId        = adId,
        amount      = amount,
        currencyCodeOpt = currencyCodeOpt,
        dateCreated = dateCreated,
        comissionPc = comissionPc,
        period      = period,
        prodContractId = prodContractId,
        rcvrAdnId   = rcvrAdnId
      )
  }

}


import MAdvReq._


case class MAdvReq(
  adId          : String,
  amount        : Float,
  currencyCodeOpt: Option[String] = None,
  comissionPc   : Option[Float],
  period        : Period,
  prodContractId: Int,
  rcvrAdnId     : String,
  dateCreated   : DateTime = DateTime.now(),
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvReq] with CurrencyCodeOpt with SiowebSqlModel[MAdvReq] with MAdvI {

  override def hasId = id.isDefined
  override def companion = MAdvReq

  override def saveInsert(implicit c: Connection): MAdvReq = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission_pc, period, prod_contract_id, rcvr_adn_id) " +
      "VALUES ({adId}, {amount}, {currencyCodeOpt}, {dateCreated}, {comissionPc}, {period}, {prodContractId}, {rcvrAdnId})")
      .on('adId -> adId, 'amount -> amount, 'currencyCodeOpt -> currencyCodeOpt, 'dateCreated -> dateCreated,
          'comissionPc -> comissionPc, 'period -> period, 'prodContractId -> prodContractId, 'rcvrAdnId -> rcvrAdnId)
      .executeInsert(rowParser single)
  }

  /**
   * Обновление существующих данных не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0

}
