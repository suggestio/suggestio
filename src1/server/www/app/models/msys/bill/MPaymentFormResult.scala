package models.msys.bill

import java.util.Currency

import io.suggest.mbill2.m.price.{MPrice, Amount_t}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.16 16:04
  * Description: Модель результата биндинга формы произвольного платежа в sys.
  */

object MPaymentFormResult {

  // Названия полей формы.
  def COMMENT_FN        = "comment"
  def AMOUNT_FN         = "amount"
  def CURRENCY_CODE_FN  = "currency_code"

}

case class MPaymentFormResult(
  amount        : Amount_t,
  currencyCode  : String,
  comment       : Option[String]
) {

  def price = MPrice(amount, Currency.getInstance(currencyCode))

}
