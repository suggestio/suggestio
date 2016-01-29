package models.adv

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 15:12
 */
package object form {

  type DatePeriod_t         = MDatesPeriod

  type DatePeriodOpt_t      = Option[DatePeriod_t]

  type QuickAdvPeriod       = QuickAdvPeriods.T

}
