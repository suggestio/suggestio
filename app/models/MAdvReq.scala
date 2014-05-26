package models

import anorm._
import MAdv._
import org.joda.time.{Period, DateTime}
import util.AnormPgInterval._
import util.AnormJodaTime._
import util.SqlModelSave
import java.sql.Connection
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 17:04
 * Description: Список запросов на размещение рекламы.
 */
object MAdvReq extends MAdvStatic[MAdvReq] {
  import SqlParser._

  val TABLE_NAME = "adv_req"

  val rowParser = ROW_PARSER_BASE ~ get[Int]("prod_contract_id") ~ get[String]("rcvr_adn_id") map {
    case id ~ adId ~ amount ~ currencyCodeOpt ~ dateCreated ~ comissionPc ~ period ~ mode ~ onStartPage ~ prodContractId ~ rcvrAdnId =>
      MAdvReq(
        id          = id,
        adId        = adId,
        amount      = amount,
        currencyCodeOpt = currencyCodeOpt,
        dateCreated = dateCreated,
        comissionPc = comissionPc,
        period      = period,
        onStartPage = onStartPage,
        prodContractId = prodContractId,
        rcvrAdnId   = rcvrAdnId
      )
  }

  /** Row-парсер выхлопа [[calculateBlockedSumForAd()]]. */
  val blockedSumParser = AMOUNT_PARSER ~ CURRENCY_PARSER map {
    case amount ~ currency => (amount, currency)
  }

  /**
   * Посчитать объём заблокированных средств на счете для ожидающих реквестов указанной рекламной карточки.
   * @param adId id рекламной карточки.
   * @return Повалютный список денег в неопределённом порядке.
   */
  def calculateBlockedSumForAd(adId: String)(implicit c: Connection): List[(Float, Currency)] = {
    SQL("SELECT SUM(amount) AS amount, currency_code FROM " + TABLE_NAME + " WHERE ad_id = {adId} GROUP BY currency_code")
      .on('adId -> adId)
      .as(blockedSumParser *)
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
  onStartPage   : Boolean,
  dateCreated   : DateTime = DateTime.now(),
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvReq] with CurrencyCodeOpt with SiowebSqlModel[MAdvReq] with MAdvI {

  override def mode = MAdvModes.REQ
  override def hasId = id.isDefined
  override def companion = MAdvReq

  override def saveInsert(implicit c: Connection): MAdvReq = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission_pc, period, mode, prod_contract_id, rcvr_adn_id) " +
      "VALUES ({adId}, {amount}, {currencyCodeOpt}, {dateCreated}, {comissionPc}, {period}, {mode}, {prodContractId}, {rcvrAdnId})")
      .on('adId -> adId, 'amount -> amount, 'currencyCodeOpt -> currencyCodeOpt, 'dateCreated -> dateCreated,
          'comissionPc -> comissionPc, 'period -> period, 'mode -> mode.toString, 'prodContractId -> prodContractId,
          'rcvrAdnId -> rcvrAdnId)
      .executeInsert(rowParser single)
  }

  /**
   * Обновление существующих данных не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0

}
