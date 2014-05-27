package models

import anorm._
import MAdv._
import org.joda.time.DateTime
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

  val rowParser = ROW_PARSER_BASE ~ get[Int]("prod_contract_id") map {
    case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ comission ~ mode ~ onStartPage ~ dateStart ~ dateEnd ~ prodAdnId ~ rcvrAdnId ~ prodContractId =>
      MAdvReq(
        id          = id,
        adId        = adId,
        amount      = amount,
        currencyCode = currencyCode,
        dateCreated = dateCreated,
        comission   = comission,
        onStartPage = onStartPage,
        prodContractId = prodContractId,
        prodAdnId   = prodAdnId,
        rcvrAdnId   = rcvrAdnId,
        dateStart   = dateStart,
        dateEnd     = dateEnd
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
  currencyCode  : String = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  comission     : Option[Float],
  prodContractId: Int,
  prodAdnId     : String,
  rcvrAdnId     : String,
  dateStart     : DateTime,
  dateEnd       : DateTime,
  onStartPage   : Boolean,
  dateCreated   : DateTime = DateTime.now(),
  id            : Pk[Int] = NotAssigned
) extends SqlModelSave[MAdvReq] with CurrencyCode with SqlModelDelete with MAdvI {

  override def mode = MAdvModes.REQ
  override def hasId = id.isDefined
  override def companion = MAdvReq
  override def dateStatus = dateCreated

  override def saveInsert(implicit c: Connection): MAdvReq = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, comission, mode, on_start_page, date_start, date_end, prod_contract_id, prod_adn_id, rcvr_adn_id) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {comission}, {mode}, {onStartPage}, {dateStart}, {dateEnd}, {prodContractId}, {prodAdnId}, {rcvrAdnId})")
      .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
          'comission -> comission, 'mode -> mode.toString, 'onStartPage -> onStartPage, 'dateStart -> dateStart,
          'dateEnd -> dateEnd, 'prodContractId -> prodContractId, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId)
      .executeInsert(rowParser single)
  }

  /**
   * Обновление существующих данных не предусмотрено.
   * @return Кол-во обновлённых рядов. Т.е. 0.
   */
  override def saveUpdate(implicit c: Connection): Int = 0

}
