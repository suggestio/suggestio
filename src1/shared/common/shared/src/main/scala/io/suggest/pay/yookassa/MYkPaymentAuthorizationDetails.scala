package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MYkPaymentAuthorizationDetails(
                                           rrn          : Option[String] = None,
                                           authCode     : Option[String] = None,
                                           threeDSecure : Option[MYkThreeDSecure] = None,
                                         )
object MYkPaymentAuthorizationDetails {

  object Fields {
    final def RRN = "rrn"
    final def AUTH_CODE = "auth_code"
    final def THREE_D_SECURE = "three_d_secure"
  }

  @inline implicit def univEq: UnivEq[MYkPaymentAuthorizationDetails] = UnivEq.derive

  implicit def paymentAuthorizationDetailsJson: OFormat[MYkPaymentAuthorizationDetails] = {
    val F = Fields
    (
      (__ \ F.RRN).formatNullable[String] and
      (__ \ F.AUTH_CODE).formatNullable[String] and
      (__ \ F.THREE_D_SECURE).formatNullable[MYkThreeDSecure]
    )(apply, unlift(unapply))
  }

}


case class MYkThreeDSecure(
                            applied: Boolean,
                          )
object MYkThreeDSecure {

  object Fields {
    final def APPLIED = "applied"
  }

  @inline implicit def univEq: UnivEq[MYkThreeDSecure] = UnivEq.derive

  implicit def threeDSecureJson: Format[MYkThreeDSecure] = {
    val F = Fields
    (__ \ F.APPLIED)
      .format[Boolean]
      .inmap( apply, unlift(unapply) )
  }

}
