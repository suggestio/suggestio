package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MYkCancellationDetails(
                                   party: String,
                                   reason: String,
                                 ) {
  override def toString =
    party + ": " + reason

}

object MYkCancellationDetails {

  object Fields {
    def PARTY = "party"
    def REASON = "reason"
  }

  @inline implicit def univEq: UnivEq[MYkCancellationDetails] = UnivEq.derive

  implicit def ykPaymentCancellationDetailsJson: OFormat[MYkCancellationDetails] = {
    val F = Fields
    (
      (__ \ F.PARTY).format[String] and
      (__ \ F.REASON).format[String]
    )(apply, unlift(unapply))
  }

}
