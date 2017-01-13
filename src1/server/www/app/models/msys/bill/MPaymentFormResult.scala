package models.msys.bill

import io.suggest.bill.{Amount_t, MCurrency, MPrice}

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
  currencyCode  : MCurrency,
  comment       : Option[String]
) {

  def price = MPrice(amount, currencyCode)

}
