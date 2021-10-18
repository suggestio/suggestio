package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MYkPaymentCancellationDetails(
                                          party: String,
                                          reason: String,
                                        )

object MYkPaymentCancellationDetails {

  object Fields {
    def PARTY = "party"
    def REASON = "reason"
  }

  @inline implicit def univEq: UnivEq[MYkPaymentCancellationDetails] = UnivEq.derive

  implicit def ykPaymentCancellationDetailsJson: OFormat[MYkPaymentCancellationDetails] = {
    val F = Fields
    (
      (__ \ F.PARTY).format[String] and
      (__ \ F.REASON).format[String]
    )(apply, unlift(unapply))
  }

}
