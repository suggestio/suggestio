package io.suggest.sjs.dt.period.m

import com.momentjs.Moment
import enumeratum._
import io.suggest.primo.IStrId
import io.suggest.dt.moment.MomentJsUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 13:02
  * Description: Модель полей интервала дат.
  */
sealed abstract class DtpInputFn extends EnumEntry with IStrId {
  def minDate: Moment = Moment()
  def monthsShown: Int
}

object DtpInputFns extends Enum[DtpInputFn] {

  override val values = findValues

  case object start extends DtpInputFn {
    override def strId = "start"
    override def monthsShown = 1
  }

  case object end extends DtpInputFn {
    override def strId = "end"
    override def minDate: Moment = super.minDate.tomorrow
    override def monthsShown = 2
  }

}
