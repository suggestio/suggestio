package io.suggest.pay.yookassa

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** @see [[https://yookassa.ru/developers/api#create_payment_confirmation]] */
case class MYkPaymentConfirmation(
                                   pcType     : MYkPaymentConfirmationType,
                                   locale     : Option[String] = None,
                                   returnUrl  : Option[String] = None,
                                   enforce    : Option[Boolean] = None,
                                   // MYkPayment only:
                                   confirmationToken: Option[String] = None,
                                   confirmationUrl: Option[String] = None,
                                   confirmationData: Option[String] = None,
                                 )

object MYkPaymentConfirmation {

  object Fields {
    final def TYPE = "type"
    final def LOCALE = "locale"
    final def RETURN_URL = "return_url"
    final def ENFORCE = "enforce"
    final def CONFIRMATION_TOKEN = "confirmation_token"
    final def CONFIRMATION_URL = "confirmation_url"
    final def CONFIRMATION_DATA = "confirmation_data"
  }

  @inline implicit def univEq: UnivEq[MYkPaymentConfirmation] = UnivEq.derive

  implicit def ykPaymentConfirmationJson: OFormat[MYkPaymentConfirmation] = {
    val F = Fields
    (
      (__ \ F.TYPE).format[MYkPaymentConfirmationType] and
      (__ \ F.LOCALE).formatNullable[String] and
      (__ \ F.RETURN_URL).formatNullable[String] and
      (__ \ F.ENFORCE).formatNullable[Boolean] and
      (__ \ F.CONFIRMATION_TOKEN).formatNullable[String] and
      (__ \ F.CONFIRMATION_URL).formatNullable[String] and
      (__ \ F.CONFIRMATION_DATA).formatNullable[String]
    )(apply, unlift(unapply))
  }

}
