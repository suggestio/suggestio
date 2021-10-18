package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** Payment creation request data.
  *
  * @see [[https://yookassa.ru/developers/api#create_payment]]
  */
case class MYkPaymentCreate(
                             amount               : MYkAmount,
                             description          : Option[String]        = None,
                             receipt              : Option[MYkReceipt]    = None,
                             recipient            : Option[MYkRecipient]  = None,
                             paymentToken         : Option[String]        = None,
                             paymentMethodId      : Option[String]        = None,
                             paymentMethodData    : Option[MYkPaymentMethodData] = None,
                             confirmation         : Option[MYkPaymentConfirmation] = None,
                             savePaymentMethod    : Option[Boolean]       = None,
                             capture              : Option[Boolean]       = None,
                             clientIp             : Option[String]        = None,
                             metadata             : Option[JsObject]      = None,
                             airline              : Option[JsObject]      = None, // TODO https://yookassa.ru/developers/api#create_payment_airline
                             transfers            : Option[JsObject]      = None, // TODO https://yookassa.ru/developers/api#create_payment_transfers
                             deal                 : Option[JsObject]      = None, // TODO https://yookassa.ru/developers/api#create_payment_deal
                             merchantCustomerId   : Option[String]        = None,
                           )

object MYkPaymentCreate {

  object Fields {
    final def AMOUNT = "amount"
    final def DESCRIPTION = "description"
    final def RECEIPT = "receipt"
    final def RECIPIENT = "recipient"
    final def PAYMENT_TOKEN = "payment_token"
    final def PAYMENT_METHOD_ID = "payment_method_id"
    final def PAYMENT_METHOD_DATA = "payment_method_data"
    final def CONFIRMATION = "confirmation"
    final def SAVE_PAYMENT_METHOD = "save_payment_method"
    final def CAPTURE = "capture"
    final def CLIENT_IP = "client_ip"
    final def METADATA = "metadata"
    final def AIRLINE = "airline"
    final def TRANSFERS = "transfers"
    final def DEAL = "deal"
    final def MERCHANT_CUSTOMER_ID = "merchant_customer_id"
  }

  @inline implicit def univEq: UnivEq[MYkPaymentCreate] = UnivEq.derive

  implicit def ykPaymentCreateJson: OFormat[MYkPaymentCreate] = {
    val F = Fields
    (
      (__ \ F.AMOUNT).format[MYkAmount] and
      (__ \ F.DESCRIPTION).formatNullable[String] and
      (__ \ F.RECEIPT).formatNullable[MYkReceipt] and
      (__ \ F.RECIPIENT).formatNullable[MYkRecipient] and
      (__ \ F.PAYMENT_TOKEN).formatNullable[String] and
      (__ \ F.PAYMENT_METHOD_ID).formatNullable[String] and
      (__ \ F.PAYMENT_METHOD_DATA).formatNullable[MYkPaymentMethodData] and
      (__ \ F.CONFIRMATION).formatNullable[MYkPaymentConfirmation] and
      (__ \ F.SAVE_PAYMENT_METHOD).formatNullable[Boolean] and
      (__ \ F.CAPTURE).formatNullable[Boolean] and
      (__ \ F.CLIENT_IP).formatNullable[String] and
      (__ \ F.METADATA).formatNullable[JsObject] and
      (__ \ F.AIRLINE).formatNullable[JsObject] and
      (__ \ F.TRANSFERS).formatNullable[JsObject] and
      (__ \ F.DEAL).formatNullable[JsObject] and
      (__ \ F.MERCHANT_CUSTOMER_ID).formatNullable[String]
    )(apply, unlift(unapply))
  }

}