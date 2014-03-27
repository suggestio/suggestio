package util

import java.text.{DecimalFormat, NumberFormat}
import java.util.Currency
import java.math.RoundingMode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.14 15:33
 * Description: Разная мелкая утиль для шаблонов.
 */
object TplDataFormatUtil {

  /** Напечатать цену согласно локали и валюте. */
  def formatPrice(price: Float, currency: Currency)(implicit ctx: Context): String = {
    // TODO следует залезать в локаль клиента и форматировать через неё?
    // TODO Нужна поддержка валют в ценах?
    val currFmt = NumberFormat.getCurrencyInstance
    currFmt.setCurrency(currency)
    currFmt.format(price)
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


  private val pcRawFloatFmt = new DecimalFormat("#.##")
  def formatPercentRaw(pc: Float)(implicit ctx: Context): String = {
    pcRawFloatFmt.format(pc)
  }

}
