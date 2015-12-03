package models.adv

import org.joda.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 15:12
 */
package object form {

  type DatePeriod_t = (LocalDate, LocalDate)

  type DatePeriodOpt_t = Option[DatePeriod_t]

  type QuickAdvPeriod       = QuickAdvPeriods.T

}
