package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/payments/payment-process#lifecycle]] */
object MYkPaymentStatuses extends StringEnum[MYkPaymentStatus] {

  case object Pending extends MYkPaymentStatus("pending")
  case object WaitingForCapture extends MYkPaymentStatus("waiting_for_capture")
  case object Succeeded extends MYkPaymentStatus("succeeded")
  case object Canceled extends MYkPaymentStatus("canceled")

  override def values = findValues

}


sealed abstract class MYkPaymentStatus(override val value: String) extends StringEnumEntry

object MYkPaymentStatus {

  @inline implicit def univEq: UnivEq[MYkPaymentStatus] = UnivEq.derive

  implicit def ykPaymentStatusJson: Format[MYkPaymentStatus] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPaymentStatuses )

}
