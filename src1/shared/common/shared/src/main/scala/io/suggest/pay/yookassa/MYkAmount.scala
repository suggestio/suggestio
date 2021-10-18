package io.suggest.pay.yookassa

import io.suggest.bill.MCurrency
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._


object MYkAmount {

  object Fields {
    final def VALUE = "value"
    final def CURRENCY = "currency"
  }

  implicit def ykAmountJson: OFormat[MYkAmount] = {
    val F = Fields
    (
      (__ \ F.VALUE).format[String] and
      (__ \ F.CURRENCY).format[MCurrency]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MYkAmount] = UnivEq.derive

}

case class MYkAmount(
                      value: String,
                      currency: MCurrency,
                    )
