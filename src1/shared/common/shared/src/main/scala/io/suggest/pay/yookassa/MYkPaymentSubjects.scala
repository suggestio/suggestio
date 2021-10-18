package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/** @see [[https://yookassa.ru/developers/54fz/parameters-values#payment-subject]] */
object MYkPaymentSubjects extends StringEnum[MYkPaymentSubject] {

  case object Commodity extends MYkPaymentSubject("commodity")
  case object Excise extends MYkPaymentSubject("excise")
  case object Job extends MYkPaymentSubject("job")
  case object Service extends MYkPaymentSubject("service")
  case object GamblingBet extends MYkPaymentSubject("gambling_bet")
  case object GamblingPrize extends MYkPaymentSubject("gambling_prize")
  case object Lottery extends MYkPaymentSubject("lottery")
  case object LotteryPrize extends MYkPaymentSubject("lottery_prize")
  case object IntellectualActivity extends MYkPaymentSubject("intellectual_activity")
  case object Payment extends MYkPaymentSubject("payment")
  case object AgentCommission extends MYkPaymentSubject("agent_commission")
  case object PropertyRight extends MYkPaymentSubject("property_right")
  case object NonOperatingGain extends MYkPaymentSubject("non_operating_gain")
  case object InsurancePremium extends MYkPaymentSubject("insurance_premium")
  case object SalesTax extends MYkPaymentSubject("sales_tax")
  case object ResortFee extends MYkPaymentSubject("resort_fee")
  case object Composite extends MYkPaymentSubject("composite")
  case object Another extends MYkPaymentSubject("another")

  override def values = findValues

}


sealed abstract class MYkPaymentSubject(override val value: String) extends StringEnumEntry

object MYkPaymentSubject {

  @inline implicit def univEq: UnivEq[MYkPaymentSubject] = UnivEq.derive

  implicit def ykPaymentSubjectJson: Format[MYkPaymentSubject] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPaymentSubjects )

}
