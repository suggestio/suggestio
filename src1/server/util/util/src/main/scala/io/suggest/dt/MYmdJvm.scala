package io.suggest.dt

import java.time.{LocalDate, OffsetDateTime}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.12.16 11:24
  * Description: Server-side утиль для работы с кросс-платформенными датами.
  */
class MYmdJvm {

  def apply(ld: LocalDate): MYmd = {
    MYmd(
      year  = ld.getYear,
      month = ld.getMonthValue,
      day   = ld.getDayOfMonth
    )
  }

  def apply(dt: OffsetDateTime): MYmd = {
    MYmd(
      year  = dt.getYear,
      month = dt.getMonthValue,
      day   = dt.getDayOfMonth
    )
  }

}
