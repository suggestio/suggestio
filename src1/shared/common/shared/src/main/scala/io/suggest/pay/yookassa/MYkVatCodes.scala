package io.suggest.pay.yookassa

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/54fz/parameters-values#vat-codes]] */
object MYkVatCodes extends IntEnum[MYkVatCode] {

  case object NoVat extends MYkVatCode(1)
  case object `0%` extends MYkVatCode(2)
  case object `10%` extends MYkVatCode(3)
  case object `20%` extends MYkVatCode(4)
  case object `10/110` extends MYkVatCode(5)
  case object `20/120` extends MYkVatCode(6)

  override def values = findValues

}


sealed abstract class MYkVatCode(override val value: Int) extends IntEnumEntry

object MYkVatCode {

  @inline implicit def univEq: UnivEq[MYkVatCode] = UnivEq.derive

  implicit def ykVatCodeJson: Format[MYkVatCode] =
    EnumeratumUtil.valueEnumEntryFormat( MYkVatCodes )

}
