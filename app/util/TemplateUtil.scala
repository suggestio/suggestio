package util

import java.text.{DecimalFormat, NumberFormat}
import java.util.Currency
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.14 15:33
 * Description: Разная мелкая утиль для шаблонов.
 */
object TplDataFormatUtil {
  // Надо укорачивать валюту до минимума
  private val CURRENCY_FIXER_RUB = "руб\\.".r
  private val CURRENCY_FIXER_USD = "USD".r

  /** Напечатать цену согласно локали и валюте. */
  def formatPrice(price: Float, currency: Currency)(implicit ctx: Context): String = {
    // TODO следует залезать в локаль клиента и форматировать через неё?
    // TODO Нужна поддержка валют в ценах?
    val currFmt = NumberFormat.getCurrencyInstance.asInstanceOf[DecimalFormat]
    currFmt.setCurrency(currency)
    val dcs = currFmt.getDecimalFormatSymbols
    val currencySymbol = formatCurrency(currency)
    dcs.setCurrencySymbol(currencySymbol)
    currFmt.setDecimalFormatSymbols(dcs)
    currFmt.format(price)
  }


  // Пока локали не поддерживаются, используется один форматтер на всех.
  private val formatPriceDigitsDF = {
    val currFmt = NumberFormat.getCurrencyInstance.asInstanceOf[DecimalFormat]
    val dcf = currFmt.getDecimalFormatSymbols
    dcf.setCurrencySymbol("")
    currFmt.setDecimalFormatSymbols(dcf)
    currFmt
  }
  def formatPriceDigits(price: Float)(implicit ctx: Context): String = {
    formatPriceDigitsDF.format(price)
  }


  /** Отформатировать валюту. */
  def formatCurrency(currency: Currency)(implicit ctx: Context): String = {
    // TODO Надо бы дедублицировать это с частями formatPrice()
    // TODO Надо прогонять код через currency formatter, чтобы учитывать локаль?
    val fmt0 = currency.getSymbol()   // TODO Надо передавать сюда локаль клиента через аргумент.
    currency.getCurrencyCode match {
      case "RUB" => CURRENCY_FIXER_RUB.replaceFirstIn(fmt0, "р.")
      case "USD" => CURRENCY_FIXER_USD.replaceFirstIn(fmt0, "$")
      case other => fmt0
    }
  }


  // Пока локали не работают, используем общий для всех форматтер данных.
  private val pcFmt = {
    val currFmt = NumberFormat.getPercentInstance
    currFmt.setMinimumFractionDigits(0)
    currFmt.setMaximumFractionDigits(2)
    currFmt
  }
  /** Напечатать долю в процентах в рамках локали. */
  def formatPercent(share: Float)(implicit ctx: Context): String = {
    // TODO Подцеплять локаль клиента
    pcFmt.format(share)
  }


  //private val pcRawFloatFmt = new DecimalFormat("#.##")
  private val pcRawIntegerFmt = new DecimalFormat("#")

  def formatPercentRaw(pc: Float): String = {
    pcRawIntegerFmt.format(pc)
  }



  private val numericDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy")

  def numericDate(dt: DateTime) = numericDateFormat.print(dt)

}
