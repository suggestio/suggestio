package util

import java.text.{DecimalFormat, NumberFormat}
import java.util.Currency
import org.joda.time.{ReadableInstant, ReadablePartial, DateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.twirl.api.{HtmlFormat, Html}
import scala.util.matching.Regex
import views.html.fc._
import views.html.helper.FieldConstructor

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

  val ELLIPSIS = "…"

  /** Сконвертить "ffffff" в List(255,255,255). */
  final def colorHex2rgb(hex: String, start: Int = 0, acc: List[Int] = Nil): List[Int] = {
    if (hex startsWith "#") {
      colorHex2rgb(hex.tail)
    } else if (start > hex.length - 1) {
      acc.reverse
    } else {
      val untilPos = start + 2
      val subhex = hex.substring(start, untilPos)
      val xint = Integer.parseInt(subhex, 16)
      colorHex2rgb(hex, untilPos, xint :: acc)
    }
  }

  /** Постпроцессинг цен. Использовать неразрывные пробелы вместо обычных. */
  def pricePostprocess(priceStr: String): String = {
    priceStr.replace('\u0020', '\u00A0')
  }

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
    currFmt.setGroupingUsed(price >= 10000)
    val formatted = currFmt.format(price)
    pricePostprocess(formatted)
  }

  /** Рендер целого числа. */
  def formatInt(number: Int)(implicit ctx: Context): String = {
    val fmt = NumberFormat.getNumberInstance
    fmt.setGroupingUsed(number >= 10000)
    fmt.format(number)
  }

  def formatIntHtml(number: Int)(implicit ctx: Context): Html = {
    Html(formatInt(number).replaceAll(" ", "&nbsp;"))
  }

  // Пока локали не поддерживаются, используется один форматтер на всех.
  def formatPriceDigits(price: Float)(implicit ctx: Context): String = {
    val formatPriceDigitsDF = {
      val currFmt = NumberFormat.getCurrencyInstance.asInstanceOf[DecimalFormat]
      val dcf = currFmt.getDecimalFormatSymbols
      dcf.setCurrencySymbol("")
      currFmt.setDecimalFormatSymbols(dcf)
      if (price <= 9999)
        currFmt.setGroupingUsed(false)
      currFmt
    }
    val formatted = formatPriceDigitsDF.format(price)
    pricePostprocess(formatted)
  }


  /** Отформатировать валюту. */
  def formatCurrency(currency: Currency)(implicit ctx: Context): String = {
    // TODO Надо бы дедублицировать это с частями formatPrice()
    // TODO Надо прогонять код через currency formatter, чтобы учитывать локаль?
    val fmt0 = currency.getSymbol()   // TODO Надо передавать сюда локаль клиента через аргумент.
    currency.getCurrencyCode match {
      case "RUB" => CURRENCY_FIXER_RUB.replaceFirstIn(fmt0, Regex.quoteReplacement("р."))
      case "USD" => CURRENCY_FIXER_USD.replaceFirstIn(fmt0, Regex.quoteReplacement("$"))
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

  /** Форматирование процентов без самого знака %%. */
  def formatPercentRaw(pc: Float): String = {
    // TODO Надо бы реоргонизовать через DecimalFormat и decimalSymbols
    pcRawIntegerFmt.format(pc)
  }


  /** Лимитирование длины строки слева. Если строка длинее указанного порога,
    * то она будет урезана и в конце появится многоточие. */
  def strLimitLen(str: String, maxLen: Int): String = {
    if (str.length <= maxLen) {
      str
    } else {
      str.substring(0, maxLen) + ELLIPSIS
    }
  }

  private val numericDateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")
  def numericDate(dt: ReadableInstant) = numericDateFormat.print(dt)
  def numericDate(d: ReadablePartial)  = numericDateFormat.print(d)

  private val numericDtFormat = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")
  // TODO Нужно прикрутить поддержку timezone, используя context
  def numericDt(dt: ReadableInstant)(implicit ctx: Context) = numericDtFormat.print(dt)

  private val numericTimeFormat = DateTimeFormat.forPattern("HH:mm:ss")
  def numericTime(dt: ReadableInstant)(implicit ctx: Context) = numericTimeFormat.print(dt)


  val firstWordRe = "(?iU)^(\\w+)\\s?(.*)$".r

  /**
   * Выделить первое слово с помощью тега. Весь текст проэкранировать.
   * @param str Исходная строка.
   * @param start Открывающий тег.
   * @param end Закрывающий тег.
   * @return Html-строка.
   */
  def highlightFirstWordEsc(str: String, start: String, end: String): Html = {
    val htmlStr = str match {
      case firstWordRe(head, tail) =>
        s"$start${HtmlFormat.escape(head)}$end ${HtmlFormat.escape(tail)}"
      case _ =>
        str
    }
    Html(htmlStr)
  }
}


/** Различные field constructor'ы. */
object FC {

  implicit val tdFc = FieldConstructor(tdFcTpl.f)

  implicit val divFc = FieldConstructor(divFcTpl.f)

  implicit val checkboxFc = FieldConstructor(checkboxFcTpl.f)

  implicit val radialFc = FieldConstructor(radialFcTpl.f)

}

