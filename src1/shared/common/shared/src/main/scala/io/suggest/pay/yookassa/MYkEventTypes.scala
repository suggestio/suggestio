package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/using-api/webhooks]] */
object MYkEventTypes extends StringEnum[MYkEventType] {

  case object PaymentSucceeded extends MYkEventType("payment.succeeded")
  case object PaymentWaitForCapture extends MYkEventType("payment.waiting_for_capture")
  case object PaymentCancelled extends MYkEventType("payment.canceled")
  case object RefundSucceeded extends MYkEventType("refund.succeeded")

  override def values = findValues

}


sealed abstract class MYkEventType( override val value: String ) extends StringEnumEntry

object MYkEventType {

  @inline implicit def univEq: UnivEq[MYkEventType] = UnivEq.derive

  implicit def ykEventTypeJson: Format[MYkEventType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkEventTypes )

}
