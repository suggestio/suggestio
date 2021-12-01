package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

object MYkDealTypes extends StringEnum[MYkDealType] {

  case object SafeDeal extends MYkDealType("safe_deal")

  override def values = findValues

}


sealed abstract class MYkDealType(override val value: String) extends StringEnumEntry

object MYkDealType {

  @inline implicit def univEq: UnivEq[MYkDealType] = UnivEq.derive

  implicit def ykDealTypeJson: Format[MYkDealType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkDealTypes )

}
