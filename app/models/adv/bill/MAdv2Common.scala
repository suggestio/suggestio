package models.adv.bill

import models.CurrencyCodeOpt
import org.joda.time.DateTime

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.15 15:55
 * Description:
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
  prodContractId  : Int,
  amount          : Float,
  currencyCode    : String          = CurrencyCodeOpt.CURRENCY_CODE_DFLT,
  dateCreated     : DateTime        = DateTime.now(),
  dateStatus      : DateTime        = DateTime.now(),
  reason          : Option[String]  = None
)

