package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

object MYkVatDataTypes extends StringEnum[MYkVatDataType] {

  case object untaxed extends MYkVatDataType("untaxed")
  case object calculated extends MYkVatDataType("calculated")
  case object mixed extends MYkVatDataType("mixed")

  override def values = findValues

}


sealed abstract class MYkVatDataType(override val value: String) extends StringEnumEntry

object MYkVatDataType {

  @inline implicit def univEq: UnivEq[MYkVatDataType] = UnivEq.derive

  implicit def ykVatDataTypeJson: Format[MYkVatDataType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkVatDataTypes )

}
