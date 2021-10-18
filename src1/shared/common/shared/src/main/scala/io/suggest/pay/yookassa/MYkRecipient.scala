package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#create_payment_recipient]] */
case class MYkRecipient(
                         gatewayId: String,
                         accountId: Option[String],  // Some() for MYkPayment, None for MYkPaymentCreate.
                       )

object MYkRecipient {

  object Fields {
    final def GATEWAY_ID = "gateway_id"
    final def ACCOUNT_ID = "account_id"
  }

  @inline implicit def univEq: UnivEq[MYkRecipient] = UnivEq.derive

  implicit def ykRecipientJson: OFormat[MYkRecipient] = {
    val F = Fields
    (
      (__ \ F.GATEWAY_ID).format[String] and
      (__ \ F.ACCOUNT_ID).formatNullable[String]
    )( apply, unlift(unapply) )
  }

}
