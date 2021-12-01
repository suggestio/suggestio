package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/api#payout_object_payout_destination_yoo_money_type]] */
object MYkPayoutDestinationTypes extends StringEnum[MYkPayoutDestinationType] {

  case object bank_card extends MYkPayoutDestinationType("bank_card")
  case object yoo_money extends MYkPayoutDestinationType("yoo_money")

  override def values = findValues

}


sealed abstract class MYkPayoutDestinationType(override val value: String) extends StringEnumEntry

object MYkPayoutDestinationType {

  @inline implicit def univEq: UnivEq[MYkPayoutDestinationType] = UnivEq.derive

  implicit def ykPaymentTypeJson: Format[MYkPayoutDestinationType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPayoutDestinationTypes )

}
