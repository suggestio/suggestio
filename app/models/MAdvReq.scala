package models

import anorm._
import MAdv._
import org.joda.time.DateTime
import util.AnormPgArray._
import util.AnormJodaTime._
import util.SqlModelSave
import java.sql.Connection
import java.util.Currency
import AdShowLevels.sls2strings

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 17:04
 * Description: Список запросов на размещение рекламы.
 */
object MAdvReq extends MAdvStatic[MAdvReq] {
  import SqlParser._

  val TABLE_NAME = "adv_req"

  val rowParser = ADV_ROW_PARSER_1 ~ get[Int]("prod_contract_id") ~ ADV_ROW_PARSER_2 map {
    case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ comission ~ mode ~ dateStart ~ dateEnd ~ prodAdnId ~
      rcvrAdnId ~ prodContractId ~ showLevels =>
      MAdvReq(
        id          = id,
        adId        = adId,
        amount      = amount,
        currencyCode = currencyCode,
        dateCreated = dateCreated,
        comission   = comission,
        prodContractId = prodContractId,
        prodAdnId   = prodAdnId,
        rcvrAdnId   = rcvrAdnId,
        dateStart   = dateStart,
        dateEnd     = dateEnd,
        showLevels  = showLevels
      )
  }

  /** Row-парсер выхлопа calculateBlockedSumForAd(). */
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
  currencyCode  : String = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  comission     : Option[Float],
  prodContractId: Int,
  prodAdnId     : String,
  rcvrAdnId     : String,
  dateStart     : DateTime,
  dateEnd       : DateTime,
  showLevels    : Set[AdShowLevel],
  dateCreated   : DateTime = DateTime.now(),
  id            : Option[Int] = None
) extends SqlModelSave[MAdvReq] with CurrencyCode with SqlModelDelete with MAdvI {

  override def mode = MAdvModes.REQ
  override def hasId = id.isDefined
  override def companion = MAdvReq
  override def dateStatus = dateCreated

  override def saveInsert(implicit c: Connection): MAdvReq = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission, mode, show_levels, date_start, date_end, prod_contract_id, prod_adn_id, rcvr_adn_id) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {comission}, {mode}, {showLevels}, {dateStart}, {dateEnd}, {prodContractId}, {prodAdnId}, {rcvrAdnId})")
      .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
          'comission -> comission, 'mode -> mode.toString, 'showLevels -> strings2pgArray(showLevels), 'dateStart -> dateStart,
          'dateEnd -> dateEnd, 'prodContractId -> prodContractId, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId)
      .executeInsert(rowParser single)
  }

  /**
   * Обновление существующих данных не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0

}
