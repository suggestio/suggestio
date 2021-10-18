package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

object MYkVatRates extends StringEnum[MYkVatRate] {

  case object `7%` extends MYkVatRate("7")
  case object `10%` extends MYkVatRate("10")
  case object `18%` extends MYkVatRate("18")
  case object `20%` extends MYkVatRate("20")

  override def values = findValues

}

sealed abstract class MYkVatRate(override val value: String) extends StringEnumEntry

object MYkVatRate {

  @inline implicit def univEq: UnivEq[MYkVatRate] = UnivEq.derive

  implicit def ykVatRateJson: Format[MYkVatRate] =
    EnumeratumUtil.valueEnumEntryFormat( MYkVatRates )

}
