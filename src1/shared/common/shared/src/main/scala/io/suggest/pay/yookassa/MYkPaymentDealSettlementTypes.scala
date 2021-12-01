package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/api#create_payment_deal_settlements_type]] */
object MYkPaymentDealSettlementTypes extends StringEnum[MYkPaymentDealSettlementType] {

  case object Payout extends MYkPaymentDealSettlementType("payout")

  override def values = findValues

}


sealed abstract class MYkPaymentDealSettlementType(override val value: String) extends StringEnumEntry

object MYkPaymentDealSettlementType {

  @inline implicit def univEq: UnivEq[MYkPaymentDealSettlementType] = UnivEq.derive

  implicit def ykPaymentDealSettlementTypeJson: Format[MYkPaymentDealSettlementType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPaymentDealSettlementTypes )

}
