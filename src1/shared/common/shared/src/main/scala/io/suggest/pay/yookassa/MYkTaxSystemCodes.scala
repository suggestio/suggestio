package io.suggest.pay.yookassa

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/54fz/parameters-values#tax-systems]] */
object MYkTaxSystemCodes extends IntEnum[MYkTaxSystemCode] {

  case object General extends MYkTaxSystemCode(1)
  case object USN extends MYkTaxSystemCode(2)
  case object `USN-Expences` extends MYkTaxSystemCode(3)
  case object ENVD extends MYkTaxSystemCode(4)
  case object ESN extends MYkTaxSystemCode(5)
  case object Patent extends MYkTaxSystemCode(6)

  override def values = findValues

}


sealed abstract class MYkTaxSystemCode(override val value: Int) extends IntEnumEntry

object MYkTaxSystemCode {

  @inline implicit def univEq: UnivEq[MYkTaxSystemCode] = UnivEq.derive

  implicit def ykTaxSystemCodeJson: Format[MYkTaxSystemCode] =
    EnumeratumUtil.valueEnumEntryFormat( MYkTaxSystemCodes )

}
