package models.adv

import java.sql.Connection
import java.util.Currency

import anorm._
import models._
import org.joda.time.DateTime
import util.SqlModelSave
import util.anorm.AnormJodaTime._
import util.anorm.AnormPgArray._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.05.14 17:04
 * Description: Список запросов на размещение рекламы.
 */
object MAdvReq extends MAdvStaticT {
  import SqlParser._

  override type T = MAdvReq

  override val TABLE_NAME = "adv_req"

  val rowParser = MAdv.ADV_ROW_PARSER_1 ~ get[Int]("prod_contract_id") ~ MAdv.SHOW_LEVELS_PARSER map {
    case id ~ adId ~ amount ~ currencyCode ~ dateCreated ~ mode ~ dateStart ~ dateEnd ~ prodAdnId ~
      rcvrAdnId ~ prodContractId ~ showLevels =>
      MAdvReq(
        id          = id,
        adId        = adId,
        amount      = amount,
        currencyCode = currencyCode,
        dateCreated = dateCreated,
        prodContractId = prodContractId,
        prodAdnId   = prodAdnId,
        rcvrAdnId   = rcvrAdnId,
        dateStart   = dateStart,
        dateEnd     = dateEnd,
        showLevels  = showLevels
      )
  }

  /** Row-парсер выхлопа calculateBlockedSumForAd(). */
  val blockedSumParser = MAdv.AMOUNT_PARSER ~ MAdv.CURRENCY_PARSER map {
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
      .as(blockedSumParser.*)
  }

}


import models.adv.MAdvReq._


final case class MAdvReq(
  override val adId               : String,
  override val amount             : Float,
  override val currencyCode       : String              = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  override val prodContractId     : Int,
  override val prodAdnId          : String,
  override val rcvrAdnId          : String,
  override val dateStart          : DateTime,
  override val dateEnd            : DateTime,
  override val showLevels         : Set[SinkShowLevel],
  override val dateCreated        : DateTime            = DateTime.now(),
  override val id                 : Option[Int]         = None
)
  extends MAdvReqT
  with SqlModelDelete
  with MAdvModelSave
{

  override def mode = MAdvModes.REQ
  override def hasId = id.isDefined
  override def companion = MAdvReq
  override def dateStatus = dateCreated


  def toRefuse(reason: String, when: DateTime = DateTime.now): MAdvRefuse = {
    MAdvRefuse(this, reason, when)
  }

}


sealed trait MAdvReqT extends SqlModelSave with MAdvI {

  override type T = MAdvReq

  /** id контракта продьюсера, по которому проходит этот запрос. */
  def prodContractId: Int

  def saveInsert(implicit c: Connection): T = {
    SQL("INSERT INTO " + TABLE_NAME +
      "(ad_id, amount, currency_code, date_created, mode, show_levels, date_start, date_end, prod_contract_id, prod_adn_id, rcvr_adn_id) " +
      "VALUES ({adId}, {amount}, {currencyCode}, {dateCreated}, {mode}, {showLevels}, {dateStart}, {dateEnd}, {prodContractId}, {prodAdnId}, {rcvrAdnId})")
      .on('adId -> adId, 'amount -> amount, 'currencyCode -> currencyCode, 'dateCreated -> dateCreated,
          'mode -> mode.toString, 'showLevels -> strings2pgArray(SinkShowLevels.sls2strings(showLevels)), 'dateStart -> dateStart,
          'dateEnd -> dateEnd, 'prodContractId -> prodContractId, 'prodAdnId -> prodAdnId, 'rcvrAdnId -> rcvrAdnId)
      .executeInsert(rowParser.single)
  }

}
