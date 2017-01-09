package io.suggest.dt

import org.joda.time.{LocalDate, ReadableDateTime}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.12.16 11:24
  * Description:
  */
class MYmdJvm {

  def apply(ld: LocalDate): MYmd = {
    MYmd(
      year  = ld.getYear,
      month = ld.getMonthOfYear,
      day   = ld.getDayOfMonth
    )
  }

  def apply(dt: ReadableDateTime): MYmd = {
    MYmd(
      year  = dt.getYear,
      month = dt.getMonthOfYear,
      day   = dt.getDayOfMonth
    )
  }

}
