package io.suggest.pay.yookassa

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

object MYkPaymentMethodTypes extends StringEnum[MYkPaymentMethodType] {

  case object alfabank extends MYkPaymentMethodType("alfabank")
  case object mobile_balance extends MYkPaymentMethodType("mobile_balance")
  case object bank_card extends MYkPaymentMethodType("bank_card")
  case object installments extends MYkPaymentMethodType("installments")
  case object cash extends MYkPaymentMethodType("cash")
  case object b2b_sberbank extends MYkPaymentMethodType("b2b_sberbank")
  case object sberbank extends MYkPaymentMethodType("sberbank")
  case object tinkoff_bank extends MYkPaymentMethodType("tinkoff_bank")
  case object yoo_money extends MYkPaymentMethodType("yoo_money")
  case object apple_pay extends MYkPaymentMethodType("apple_pay")
  case object google_pay extends MYkPaymentMethodType("google_pay")
  case object qiwi extends MYkPaymentMethodType("qiwi")
  case object wechat extends MYkPaymentMethodType("wechat")
  case object webmoney extends MYkPaymentMethodType("webmoney")

  override def values = findValues
}


sealed abstract class MYkPaymentMethodType(override val value: String) extends StringEnumEntry

object MYkPaymentMethodType {

  @inline implicit def univEq: UnivEq[MYkPaymentMethodType] = UnivEq.derive

  implicit def ykPaymentTypeJson: Format[MYkPaymentMethodType] =
    EnumeratumUtil.valueEnumEntryFormat( MYkPaymentMethodTypes )

}
