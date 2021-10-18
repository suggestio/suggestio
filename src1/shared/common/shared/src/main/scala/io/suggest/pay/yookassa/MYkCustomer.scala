package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MYkCustomer(
                        fullName    : Option[String]    = None,
                        inn         : Option[String]    = None,
                        email       : Option[String]    = None,
                        phone       : Option[String]    = None,
                      )

object MYkCustomer {

  @inline implicit def univEq: UnivEq[MYkCustomer] = UnivEq.derive

  object Fields {
    final def FULL_NAME = "full_name"
    final def INN = "inn"
    final def EMAIL = "email"
    final def PHONE = "phone"
  }

  implicit def ykCustomerJson: OFormat[MYkCustomer] = {
    val F = Fields
    (
      (__ \ F.FULL_NAME).formatNullable[String] and
        (__ \ F.INN).formatNullable[String] and
        (__ \ F.EMAIL).formatNullable[String] and
        (__ \ F.PHONE).formatNullable[String]
      )(apply, unlift(unapply))
  }

}
