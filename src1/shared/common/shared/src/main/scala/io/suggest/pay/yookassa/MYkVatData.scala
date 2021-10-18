package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MYkVatData(
                       vatType  : MYkVatDataType,
                       amount   : Option[MYkAmount]   = None,
                       rate     : Option[MYkVatRate]  = None,
                     )

object MYkVatData {

  object Fields {
    final def VAT_TYPE = "type"
    final def AMOUNT = "amount"
    final def RATE = "rate"
  }

  @inline implicit def univEq: UnivEq[MYkVatData] = UnivEq.derive

  implicit def ykVatDataJson: OFormat[MYkVatData] = {
    val F = Fields
    (
      (__ \ F.VAT_TYPE).format[MYkVatDataType] and
      (__ \ F.AMOUNT).formatNullable[MYkAmount] and
      (__ \ F.RATE).formatNullable[MYkVatRate]
    )(apply, unlift(unapply))
  }

}
