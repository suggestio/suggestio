package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/api#create_payment_confirmation]] */
object MYkPaymentConfirmationTypes extends StringEnum[MYkPaymentConfirmationType] {

  case object Embedded extends MYkPaymentConfirmationType("embedded")
  case object External extends MYkPaymentConfirmationType("external")
  case object MobileApplication extends MYkPaymentConfirmationType("mobile_application")
  case object QrCode extends MYkPaymentConfirmationType("qr")
  case object Redirect extends MYkPaymentConfirmationType("redirect")

  override def values = findValues

}


sealed abstract class MYkPaymentConfirmationType(override val value: String) extends StringEnumEntry

object MYkPaymentConfirmationType {

  @inline implicit def univEq: UnivEq[MYkPaymentConfirmationType] = UnivEq.derive

  implicit def ykPaymentConfirmationTypeJson: Format[MYkPaymentConfirmationType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPaymentConfirmationTypes )

}
