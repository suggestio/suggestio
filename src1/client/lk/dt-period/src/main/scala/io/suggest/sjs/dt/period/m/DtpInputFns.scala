package io.suggest.sjs.dt.period.m

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.dt.interval.MRangeYmd

import java.time.LocalDate

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 13:02
  * Description: Модель полей интервала дат.
  */

object DtpInputFns extends StringEnum[DtpInputFn] {

  case object start extends DtpInputFn("start") {
    override def withTodayBtn = true
    override def monthsShown = 1
    override def minDate(customRangeOpt: Option[MRangeYmd]): LocalDate = {
      LocalDate.now()
    }
    override def selectsStart: Boolean = true
  }

  case object end extends DtpInputFn("end") {
    override def withTodayBtn = false
    override def minDate(customRangeOpt: Option[MRangeYmd]): LocalDate = {
      val ld0 = customRangeOpt.fold( LocalDate.now() ) { cr =>
        cr.dateStart.to[LocalDate]
      }
      ld0.plusDays(1)
    }
    override def monthsShown = 3
    override def maxDate: LocalDate = {
      super.maxDate.plusDays(1)
    }
    override def selectsStart: Boolean = false
  }

  override val values = findValues

}


sealed abstract class DtpInputFn(override val value: String) extends StringEnumEntry {
  def minDate(customRangeOpt: Option[MRangeYmd]): LocalDate
  def monthsShown: Int
  def withTodayBtn: Boolean
  def maxDate: LocalDate = {
    LocalDate
      .now()
      .plusYears(1)
  }
  def selectsStart: Boolean
  def selectsEnd  : Boolean = !selectsStart
}

