package util

import java.text.{DecimalFormat, NumberFormat}
import java.util.Currency
import java.math.RoundingMode
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
  val CURRENCY_FIXER_RUB = " руб\\.".r
  val CURRENCY_FIXER_USD = " USD".r

  /** Напечатать цену согласно локали и валюте. */
  def formatPrice(price: Float, currency: Currency)(implicit ctx: Context): String = {
    // TODO следует залезать в локаль клиента и форматировать через неё?
    // TODO Нужна поддержка валют в ценах?
    val currFmt = NumberFormat.getCurrencyInstance
    currFmt.setCurrency(currency)
    val fmt0 = currFmt.format(price)
    // Укоротить валюту
    currency.getCurrencyCode match {
      case "RUB" => CURRENCY_FIXER_RUB.replaceFirstIn(fmt0, "р.")
      case "USD" => CURRENCY_FIXER_USD.replaceFirstIn(fmt0, "$")    // TODO Баксы вроде бы перед ценой отображаются, а не после.
      case _ => fmt0
    }
  }


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
