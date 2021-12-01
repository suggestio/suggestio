package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/api#deal_object_safe_deal_status]] */
object MYkDealStatuses extends StringEnum[MYkDealStatus] {

  case object Opened extends MYkDealStatus("opened")

  case object Closed extends MYkDealStatus("closed")


  override def values = findValues

}


sealed abstract class MYkDealStatus(override val value: String) extends StringEnumEntry

object MYkDealStatus {

  @inline implicit def univEq: UnivEq[MYkDealStatus] = UnivEq.derive

  implicit def ykDealStatusJson: Format[MYkDealStatus] =
    EnumeratumUtil.valueEnumEntryFormat( MYkDealStatuses )

}
