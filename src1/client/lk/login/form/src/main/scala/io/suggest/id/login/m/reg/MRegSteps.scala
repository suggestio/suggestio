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

  case object S0Creds extends MRegStep(0) {
    override def stepLabelCode = "step 0 req-s"
  }
  case object S1Captcha extends MRegStep(1) {
    override def stepLabelCode = "step 1 captcha"
  }
  case object S2SmsCode extends MRegStep(2) {
    override def stepLabelCode = "step 2 sms"
  }
  case object S3CheckBoxes extends MRegStep(3) {
    override def stepLabelCode = "step 3 checkboxes"
  }

  override val values = findValues
}


sealed abstract class MRegStep(override val value: Int) extends IntEnumEntry {
  def stepLabelCode: String
}

object MRegStep {
  @inline implicit def univEq: UnivEq[MRegStep] = UnivEq.derive
}
