package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/api#payout_object_status]] */
object MYkPayoutStatuses extends StringEnum[MYkPayoutStatus] {

  case object Pending extends MYkPayoutStatus( "pending" )
  case object Succeeded extends MYkPayoutStatus( "succeeded" )
  case object Canceled extends MYkPayoutStatus( "canceled" )

  override def values = findValues

}


sealed abstract class MYkPayoutStatus(override val value: String) extends StringEnumEntry

object MYkPayoutStatus {

  @inline implicit def univEq: UnivEq[MYkPayoutStatus] = UnivEq.derive

  implicit def ykPayoutStatusJson: Format[MYkPayoutStatus] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPayoutStatuses )

}
