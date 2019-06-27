package io.suggest.id.login.m.reg

import enumeratum.values.{IntEnum, IntEnumEntry}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 15:32
  * Description: Модель шагов регистрации.
  */
object MRegSteps extends IntEnum[MRegStep] {

  case object S0Creds extends MRegStep(0)
  case object S1Captcha extends MRegStep(1)
  case object S2SmsCode extends MRegStep(2)
  case object S3CheckBoxes extends MRegStep(3)
  case object S4SetPassword extends MRegStep(4)

  override val values = findValues

}


sealed abstract class MRegStep(override val value: Int) extends IntEnumEntry

object MRegStep {
  @inline implicit def univEq: UnivEq[MRegStep] = UnivEq.derive
}
