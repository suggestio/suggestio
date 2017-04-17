package models.adv.direct

import java.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.04.15 13:12
 * Description: Один результат маппинга geo-adv-формы размещения рекламной карточки.
 * Результат содержит в себе целевой узел и параметры размещения карточки на нем.
 */
case class AdvFormEntry(
  adnId     : String,
  advertise : Boolean,
  dateStart : LocalDate,
  dateEnd   : LocalDate
)
  extends IAdvTerms
