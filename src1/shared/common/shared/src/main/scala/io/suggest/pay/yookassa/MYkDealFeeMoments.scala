package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/api#deal_object_safe_deal_fee_moment]] */
object MYkDealFeeMoments extends StringEnum[MYkDealFeeMoment] {

  case object PaymentSucceeded extends MYkDealFeeMoment("payment_succeeded")

  case object DealClosed extends MYkDealFeeMoment("deal_closed")


  override def values = findValues

}


sealed abstract class MYkDealFeeMoment(override val value: String) extends StringEnumEntry

object MYkDealFeeMoment {

  @inline implicit def univEq: UnivEq[MYkDealFeeMoment] = UnivEq.derive

  implicit def ykDealFeeMomentJson: Format[MYkDealFeeMoment] =
    EnumeratumUtil.valueEnumEntryFormat( MYkDealFeeMoments )

}
