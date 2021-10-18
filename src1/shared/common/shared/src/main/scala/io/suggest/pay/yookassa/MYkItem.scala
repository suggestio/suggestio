package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._


/** @see https://yookassa.ru/developers/api#create_payment_receipt_items */
case class MYkItem(
                    description               : String,
                    quantity                  : String,
                    amount                    : MYkAmount,
                    vatCode                   : MYkVatCode,
                    paymentSubject            : Option[MYkPaymentSubject] = None,
                    paymentMode               : Option[MYkPaymentMode]    = None,
                    productCode               : Option[String]            = None,
                    countryOfOriginCode       : Option[String]            = None,
                    customsDeclarationNumber  : Option[String]            = None,
                    excise                    : Option[String]            = None,
                  )

object MYkItem {

  object Fields {
    final def DESCRIPTION = "description"
    final def QUANTITY = "quantity"
    final def AMOUNT = "amount"
    final def VAT_CODE = "vat_code"
    final def PAYMENT_SUBJECT = "payment_subject"
    final def PAYMENT_MODE = "payment_mode"
    final def PRODUCT_CODE = "product_code"
    final def COUNTRY_OF_ORIGIN_CODE = "country_of_origin_code"
    final def CUSTOMS_DECLARATION_NUMBER = "customs_declaration_number"
    final def EXCISE = "excise"
  }

  @inline implicit def univEq: UnivEq[MYkItem] = UnivEq.derive

  implicit def ykItemJson: OFormat[MYkItem] = {
    val F = Fields
    (
      (__ \ F.DESCRIPTION).format[String] and
      (__ \ F.QUANTITY).format[String] and
      (__ \ F.AMOUNT).format[MYkAmount] and
      (__ \ F.VAT_CODE).format[MYkVatCode] and
      (__ \ F.PAYMENT_SUBJECT).formatNullable[MYkPaymentSubject] and
      (__ \ F.PAYMENT_MODE).formatNullable[MYkPaymentMode] and
      (__ \ F.PRODUCT_CODE).formatNullable[String] and
      (__ \ F.COUNTRY_OF_ORIGIN_CODE).formatNullable[String] and
      (__ \ F.CUSTOMS_DECLARATION_NUMBER).formatNullable[String] and
      (__ \ F.EXCISE).formatNullable[String]
    )(apply, unlift(unapply))
  }

}
