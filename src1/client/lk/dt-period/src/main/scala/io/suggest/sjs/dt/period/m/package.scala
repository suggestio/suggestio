package io.suggest.sjs.interval

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 16:39
 */
package object m {

  type DatesPeriod_t = (String, String)

  type PeriodEith_t  = Either[DatesPeriod_t, String]

}
