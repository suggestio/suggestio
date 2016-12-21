package io.suggest.sjs.dt.period.m

import enumeratum._
import io.suggest.primo.IStrId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 13:02
  * Description: Модель полей интервала дат.
  */
sealed abstract class DtpInputFn extends EnumEntry with IStrId

object DtpInputFns extends Enum[DtpInputFn] {

  override val values = findValues

  case object start extends DtpInputFn {
    override def strId = "start"
  }

  case object end extends DtpInputFn {
    override def strId = "end"
  }

}
