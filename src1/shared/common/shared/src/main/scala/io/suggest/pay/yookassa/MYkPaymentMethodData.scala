package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

case class MYkPaymentMethodData(
                                 pmType              : MYkPaymentMethodType,
                                 login               : Option[String] = None,
                                 phone               : Option[String] = None,
                                 card                : Option[String] = None,
                                 paymentPurpose      : Option[String] = None,
                                 vatData             : Option[MYkVatData] = None,
                                 paymentData         : Option[String] = None,
                                 paymentMethodToken  : Option[String] = None,
                                 // MYkPayment only:
                                 id                  : Option[String] = None,
                                 saved               : Option[Boolean] = None,
                                 title               : Option[String] = None,
                                 payerBankDetails    : Option[JsObject] = None,  // TODO https://yookassa.ru/developers/api#payment_object_payment_method_b2b_sberbank_payer_bank_details
                                 accountNumber       : Option[String] = None,
                               )

object MYkPaymentMethodData {

  object Fields {
    final def TYPE = "type"
    final def LOGIN = "login"
    final def PHONE = "phone"
    final def CARD = "card"
    final def PAYMENT_PURPOSE = "payment_purpose"
    final def VAT_DATA = "vat_data"
    final def PAYMENT_DATA = "payment_data"
    final def PAYMENT_METHOD_TOKEN = "payment_method_token"

    final def ID = "id"
    final def SAVED = "saved"
    final def TITLE = "title"
    final def PAYER_BANK_DETAILS = "payer_bank_details"
    final def ACCOUNT_NUMBER = "account_number"
  }

  @inline implicit def univEq: UnivEq[MYkPaymentMethodData] = UnivEq.derive

  implicit def ykPaymentMethodDataJson: OFormat[MYkPaymentMethodData] = {
    val F = Fields
    (
      (__ \ F.TYPE).format[MYkPaymentMethodType] and
      (__ \ F.LOGIN).formatNullable[String] and
      (__ \ F.PHONE).formatNullable[String] and
      (__ \ F.CARD).formatNullable[String] and
      (__ \ F.PAYMENT_PURPOSE).formatNullable[String] and
      (__ \ F.VAT_DATA).formatNullable[MYkVatData] and
      (__ \ F.PAYMENT_DATA).formatNullable[String] and
      (__ \ F.PAYMENT_METHOD_TOKEN).formatNullable[String] and
      (__ \ F.ID).formatNullable[String] and
      (__ \ F.SAVED).formatNullable[Boolean] and
      (__ \ F.TITLE).formatNullable[String] and
      (__ \ F.PAYER_BANK_DETAILS).formatNullable[JsObject] and
      (__ \ F.ACCOUNT_NUMBER).formatNullable[String]
    )(apply, unlift(unapply))
  }

}
