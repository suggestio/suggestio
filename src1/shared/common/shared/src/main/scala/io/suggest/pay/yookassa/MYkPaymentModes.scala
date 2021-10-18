package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/54fz/parameters-values#payment-mode]] */
object MYkPaymentModes extends StringEnum[MYkPaymentMode] {
  case object FullPrePayment extends MYkPaymentMode("full_prepayment")
  case object PartialPrePayment extends MYkPaymentMode("partial_prepayment")
  case object Advance extends MYkPaymentMode("advance")
  case object FullPayment extends MYkPaymentMode("full_payment")
  case object PartialPayment extends MYkPaymentMode("partial_payment")
  case object Credit extends MYkPaymentMode("credit")
  case object CreditPayment extends MYkPaymentMode("credit_payment")

  override def values = findValues
}


sealed abstract class MYkPaymentMode(override val value: String) extends StringEnumEntry

object MYkPaymentMode {
  @inline implicit def univEq: UnivEq[MYkPaymentMode] = UnivEq.derive

  implicit def ykPaymentModeJson: Format[MYkPaymentMode] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPaymentModes )

}
