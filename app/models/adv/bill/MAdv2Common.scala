package models.adv.bill

import java.util.Currency

import com.google.inject.Singleton
import models.CurrencyCodeOpt
import org.joda.time.DateTime
import anorm._

import util.anorm.AnormJodaTime._
import util.sqlm.SqlFieldsGenerators

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:55
 * Description: common-поля модели размещений.
 */
@Singleton
class MAdv2Common_ { that =>

  /** Поля (колонки) модели. */
  object Fields extends SqlFieldsGenerators {

    val MODE_FN               = "mode"
    val AD_ID_FN              = "ad_id"
    val DATE_START_FN         = "date_start"
    val DATE_END_FN           = "date_end"
    val PRODUCER_ID_FN        = "prod_id"
    val PROD_CONTRACT_ID_FN   = "prod_contract_id"
    val AMOUNT_FN             = "amount"
    val CURRENCY_CODE_FN      = "currency_code"
    val DATE_CREATED_FN       = "date_created"
    val DATE_STATUS_FN        = "date_status"
    val REASON_FN             = "reason"

    /** Все поля этой модели в основном порядке. */
    override val FIELDS: Seq[String] = {
      Seq(
        MODE_FN, AD_ID_FN, DATE_START_FN, DATE_END_FN, PRODUCER_ID_FN, PROD_CONTRACT_ID_FN,
        AMOUNT_FN, CURRENCY_CODE_FN, DATE_CREATED_FN, DATE_STATUS_FN, REASON_FN
      )
    }

  }


  /** SQL-парсеры модели для десериализации элементов. */
  object Parsers {

    import anorm.SqlParser._
    import Fields._

    val MODE                  = str(MODE_FN).map(MAdv2Modes.withName(_): MAdv2Mode)
    val AD_ID                 = str(AD_ID_FN)
    val DATE_START            = get[DateTime](DATE_START_FN)
    val DATE_END              = get[DateTime](DATE_END_FN)
    val PRODUCER_ID           = str(PRODUCER_ID_FN)
    val PROD_CONTRACT_ID      = get[Option[Int]](PROD_CONTRACT_ID_FN)
    val AMOUNT                = float(AMOUNT_FN)
    val CURRENCY_CODE         = str(CURRENCY_CODE_FN)
    val CURRENCY              = CURRENCY_CODE.map { Currency.getInstance }
    val DATE_CREATED          = get[DateTime](DATE_CREATED_FN)
    val DATE_STATUS           = get[DateTime](DATE_STATUS_FN)
    val REASON                = get[Option[String]](REASON_FN)

    val ROW = {
      val p = MODE ~ AD_ID ~ DATE_START ~ DATE_END ~ PRODUCER_ID ~ PROD_CONTRACT_ID ~ AMOUNT ~ CURRENCY_CODE ~ DATE_CREATED ~ DATE_STATUS ~ REASON
      p.map {
        case mode ~ adId ~ dateStart ~ dateEnd ~ producerId ~ prodContractId ~ amount ~ currencyCode ~ dateCreated ~ dateStatus ~ reason  =>
          MAdv2Common(
            mode            = mode,
            adId            = adId,
            dateStart       = dateStart,
            dateEnd         = dateEnd,
            producerId      = producerId,
            prodContractId  = prodContractId,
            amount          = amount,
            currencyCode    = currencyCode,
            dateCreated     = dateCreated,
            dateStatus      = dateStatus,
            reason          = reason
          )
      }
    }

  }


  /**
   * Сериализация экземпляров модели: параметры для anorm для SQL.on() INSERT/UPDATE.
   * @param o Инстанс этой модели.
   * @return Список, пригодный для передачи в SQL.on().
   */
  def anormParams(o: MAdv2Common): List[NamedParameter] = {
    import Fields._
    List[NamedParameter](
      MODE_FN               -> o.mode.strId,
      AD_ID_FN              -> o.adId,
      DATE_START_FN         -> o.dateStart,
      DATE_END_FN           -> o.dateEnd,
      PRODUCER_ID_FN        -> o.producerId,
      PROD_CONTRACT_ID_FN   -> o.producerId,
      AMOUNT_FN             -> o.amount,
      CURRENCY_CODE_FN      -> o.currencyCode,
      DATE_CREATED_FN       -> o.dateCreated,
      DATE_STATUS_FN        -> o.dateStatus,
      REASON_FN             -> o.reason
    )
  }

}


/**
 * Экземпляр модели, содержащий общую информации о платном размещении.
 * @param mode Текущее состояние обработки размещения.
 * @param adId id размещаемой рекламной карточки.
 * @param dateStart Дата начала размещения.
 * @param dateEnd Дата окончания размещения.
 * @param producerId id продьюсера.
 * @param prodContractId id контракта продьюсера.
 * @param amount Рассчитанная стоимость размещения.
 * @param currencyCode Валюта стоимости размещения.
 * @param dateCreated Дата создания размещения.
 * @param dateStatus Дата последнего изменения статуса.
 * @param reason Причина отказа в размещении, если mode = отказ.
 */
case class MAdv2Common(
  mode            : MAdv2Mode,
  adId            : String,
  dateStart       : DateTime,
  dateEnd         : DateTime,
  producerId      : String,
  prodContractId  : Option[Int],
  amount          : Float,
  currencyCode    : String          = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  dateCreated     : DateTime        = DateTime.now(),
  dateStatus      : DateTime        = DateTime.now(),
  reason          : Option[String]  = None
)

