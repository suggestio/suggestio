package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** @see [[https://yookassa.ru/developers/api#payment_object]] */
case class MYkPayment(
                       id                     : String,
                       status                 : MYkPaymentStatus,
                       amount                 : MYkAmount,
                       incomeAmount           : Option[MYkAmount] = None,
                       description            : Option[String] = None,
                       recipient              : MYkRecipient,
                       paymentMethod          : Option[MYkPaymentMethodData],
                       capturedAt             : Option[String] = None,
                       createdAt              : String,
                       expiresAt              : Option[String] = None,
                       confirmation           : Option[MYkPaymentConfirmation] = None,
                       test                   : Boolean,
                       refundedAmount         : Option[MYkAmount] = None,
                       paid                   : Boolean,
                       refundable             : Boolean,
                       receiptRegistration    : Option[String] = None,
                       metadata               : Option[JsObject] = None,
                       cancellationDetails    : Option[MYkCancellationDetails] = None,
                       authorizationDetails   : Option[MYkPaymentAuthorizationDetails] = None,
                       transfers              : Option[JsObject] = None,
                       deal                   : Option[MYkPaymentDeal] = None,
                       merchantCustomerId     : Option[String] = None,
                       // TODO -- 22 args here -- play-json not compatible with 23+-arg classes.
                     )


object MYkPayment {

  @inline implicit def univEq: UnivEq[MYkPayment] = UnivEq.derive

  implicit def ykPaymentJson: OFormat[MYkPayment] = {
    (
      (__ \ "id").format[String] and
      (__ \ "status").format[MYkPaymentStatus] and
      (__ \ "amount").format[MYkAmount] and
      (__ \ "income_amount").formatNullable[MYkAmount] and
      (__ \ "description").formatNullable[String] and
      (__ \ "recipient").format[MYkRecipient] and
      (__ \ "payment_method").formatNullable[MYkPaymentMethodData] and
      (__ \ "captured_at").formatNullable[String] and
      (__ \ "created_at").format[String] and
      (__ \ "expires_at").formatNullable[String] and
      (__ \ "confirmation").formatNullable[MYkPaymentConfirmation] and
      (__ \ "test").format[Boolean] and
      (__ \ "refunded_amount").formatNullable[MYkAmount] and
      (__ \ "paid").format[Boolean] and
      (__ \ "refundable").format[Boolean] and
      (__ \ "receipt_registration").formatNullable[String] and
      (__ \ "metadata").formatNullable[JsObject] and
      (__ \ "cancellation_details").formatNullable[MYkCancellationDetails] and
      (__ \ "authorization_details").formatNullable[MYkPaymentAuthorizationDetails] and
      (__ \ "transfers").formatNullable[JsObject] and
      (__ \ "deal").formatNullable[MYkPaymentDeal] and
      (__ \ "merchant_customer_id").formatNullable[String]
    )(apply, unlift(unapply))
  }

}
