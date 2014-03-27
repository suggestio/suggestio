package util

import java.text.NumberFormat
import java.util.Currency

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.14 15:33
 * Description: Разная мелкая утиль для шаблонов.
 */
object TplDataFormatUtil {

  def formatPrice(price: Float, currency: Currency)(implicit ctx: Context): String = {
    // TODO следует залезать в локаль клиента и форматировать через неё?
    // TODO Нужна поддержка валют в ценах?
    val currFmt = NumberFormat.getCurrencyInstance
    currFmt.setCurrency(currency)
    currFmt.format(price)
  }

  def formatPercent(pc: Float)(implicit ctx: Context): String = {
    // TODO Подцеплять локаль клиента
    val currFmt = NumberFormat.getPercentInstance
    currFmt.setMaximumFractionDigits(0)
    currFmt.format(pc)
  }
}
