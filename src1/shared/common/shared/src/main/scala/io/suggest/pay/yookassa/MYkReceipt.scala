package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MYkReceipt(
                       items          : Seq[MYkItem],
                       customer       : Option[MYkCustomer]       = None,
                       taxSystemCode  : Option[MYkTaxSystemCode]  = None,
                       phone          : Option[String]            = None,
                       email          : Option[String]            = None,
                     )

object MYkReceipt {

  object Fields {
    final def ITEMS = "items"
    final def CUSTOMER = "customer"
    final def TAX_SYSTEM_CODE = "tax_system_code"
    final def PHONE = "phone"
    final def EMAIL = "email"
  }

  @inline implicit def univEq: UnivEq[MYkReceipt] = UnivEq.derive

  implicit def ykReceiptJson: OFormat[MYkReceipt] = {
    val F = Fields
    (
      (__ \ F.ITEMS).format[Seq[MYkItem]] and
      (__ \ F.CUSTOMER).formatNullable[MYkCustomer] and
      (__ \ F.TAX_SYSTEM_CODE).formatNullable[MYkTaxSystemCode] and
      (__ \ F.PHONE).formatNullable[String] and
      (__ \ F.EMAIL).formatNullable[String]
    )(apply, unlift(unapply))
  }

}