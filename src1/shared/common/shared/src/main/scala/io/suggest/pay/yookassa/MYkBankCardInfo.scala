package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#payment_object_payment_method_bank_card_card]] */
case class MYkBankCardInfo(
                        first6          : Option[String]    = None,
                        last4           : Option[String]    = None,
                        expiryMonth     : Option[String]    = None,
                        expiryYear      : Option[String]    = None,
                        cardType        : Option[String]    = None,
                        issuerCountry   : Option[String]    = None,
                        issuerName      : Option[String]    = None,
                      )


object MYkBankCardInfo {

  @inline implicit def univEq: UnivEq[MYkBankCardInfo] = UnivEq.derive

  implicit def ykBankCardJson: OFormat[MYkBankCardInfo] = {
    (
      (__ \ "first6").formatNullable[String] and
      (__ \ "last4").formatNullable[String] and
      (__ \ "expiry_month").formatNullable[String] and
      (__ \ "expiry_year").formatNullable[String] and
      (__ \ "card_type").formatNullable[String] and
      (__ \ "issuer_country").formatNullable[String] and
      (__ \ "issuer_name").formatNullable[String]
    )(apply, unlift(unapply))
  }

}
