package io.suggest.sjs.dt.period.m

import com.momentjs.Moment
import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.dt.interval.MRangeYmd
import io.suggest.dt.moment.MomentJsUtil.Implicits._

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
    override def minDate(customRangeOpt: Option[MRangeYmd]): Moment = {
      Moment()
    }
    override def selectsStart: Boolean = true
  }

  case object end extends DtpInputFn("end") {
    override def withTodayBtn = false
    override def minDate(customRangeOpt: Option[MRangeYmd]): Moment = {
      customRangeOpt.fold(Moment().tomorrow) { cr =>
        cr.dateStart.to[Moment].tomorrow
      }
    }
    override def monthsShown = 2
    override def maxDate: Moment = super.maxDate.tomorrow
    override def selectsStart: Boolean = false
  }

  override val values = findValues

}


sealed abstract class DtpInputFn(override val value: String) extends StringEnumEntry {
  def minDate(customRangeOpt: Option[MRangeYmd]): Moment
  def monthsShown: Int
  def withTodayBtn: Boolean
  def maxDate: Moment = {
    Moment().plusYears(1)
  }
  def selectsStart: Boolean
  def selectsEnd  : Boolean = !selectsStart
}

