package util

import java.text.{DecimalFormat, NumberFormat}

import io.suggest.bill.{MCurrencies, MCurrency, MPrice}
import io.suggest.common.html.HtmlConstants
import io.suggest.common.text.StringUtil
import models.mctx.Context
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ReadableInstant, ReadablePartial}
import play.twirl.api.{Html, HtmlFormat}
import views.html.fc._
import views.html.helper.FieldConstructor

import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.14 15:33
 * Description: Разная мелкая утиль для шаблонов.
 */
object TplDataFormatUtil {

  val ELLIPSIS = "…"

  /** Сконвертить "ffffff" в List(255,255,255). */
  def colorHex2rgb(hex: String): List[Int] = {
    colorHex2rgb(hex, 0, Nil)
  }
  def colorHex2rgb(hex: String, start: Int, acc: List[Int]): List[Int] = {
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

  /** Отрендерить набор цветов в rgb(...) или rgba(...) цвет для css-значений. */
  def formatRgbHexColorCss(colorHex: String, withOpacity: Option[Float] = None): String = {
    formatRgbColorCss(colorHex2rgb(colorHex), withOpacity)
  }
  def formatRgbColorCss(rgb: TraversableOnce[Int], withOpacity: Option[Float] = None): String = {
    val withOp = withOpacity.isDefined
    val l0 = if (withOp) 22 else 16
    val sb = new StringBuilder(l0, "rgb")
    if (withOp)
      sb.append('a')
    sb.append('(')
    for (c <- rgb) {
      sb.append(c)
        .append(',')
    }
    if (withOp) {
      sb.append(withOpacity.get)
    } else {
      sb.length = sb.length - 1
    }
    sb.append(')')
      .toString
  }

  def colorRgb2Hsl(rgb: List[Int]): List[Int] = {

    val r: Float = rgb(0).toFloat / 255
    val g: Float = rgb(1).toFloat / 255
    val b: Float = rgb(2).toFloat / 255

    val rgbSorted = List(r,g,b).sortWith(_ < _)

    val max = rgbSorted(2)
    val min = rgbSorted(0)

    if (max == min) {
      List( 0, 0, ((max + min)/2*100).toInt)
    } else {

      val l = ( max + min ) / 2

      val s = if( l < 0.5 ){
        (max - min) / (max + min)
      } else {
        (max - min) / (2.0 - max - min)
      }

      val h = if( r == max ){
        (g - b) / (max - min)
      } else if( g == max ){
        2.0 + (b - r) / ( max - min )
      } else {
        4.0 + (r - g) / ( max - min )
      }

      val h1 = if( h < 0 ) h*60 + 360 else h*60

      List( h1.toInt, (s*100).toInt, (l*100).toInt )

    }

  }

  import HtmlConstants.NBSP

  def NUMBER_GROUPING_THRESHOLD = 10000

  /** Постпроцессинг цен. Использовать неразрывные пробелы вместо обычных. */
  def pricePostprocess(priceStr: String): String = {
    priceStr.replace('\u0020', NBSP)
  }

  def formatPrice(price: MPrice)(implicit ctx: Context): String = {
    val currFmt = NumberFormat.getCurrencyInstance( ctx.messages.lang.locale )
      .asInstanceOf[DecimalFormat]
    currFmt.setCurrency(price.currency.toJavaCurrency)
    val currencySymbol = formatCurrency(price.currency)
    val dcs = currFmt.getDecimalFormatSymbols
    dcs.setCurrencySymbol(currencySymbol)
    dcs.setGroupingSeparator(NBSP)
    // TODO выставить остальные сепараторы, чтобы не вызывать pricePostprocess()
    currFmt.setDecimalFormatSymbols(dcs)
    currFmt.setGroupingUsed(price.amount >= NUMBER_GROUPING_THRESHOLD)
    val formatted = currFmt.format(price.amount)
    pricePostprocess(formatted)
  }

  /** Напечатать цену согласно локали и валюте. */
  def formatPrice(price: Float, currency: MCurrency)(implicit ctx: Context): String = {
    formatPrice(price.toDouble, currency)
  }
  def formatPrice(price: Double, currency: MCurrency)(implicit ctx: Context): String = {
    formatPrice( MPrice(price, currency) )
  }
  def formatPrice(price: Double, currencyCode: String)(implicit ctx: Context): String = {
    formatPrice(price, MCurrencies.withName(currencyCode))
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

  /**
    * Форматирование amount стоимости в строку БЕЗ символа валюты.
    * @param mprice Сумма.
    * @param ctx Контекст рендера.
    * @return Строка вида "10 034"
    */
  def formatPriceAmount(mprice: MPrice)(implicit ctx: Context): String = {
    val formatPriceDigitsDF = {
      val currFmt = NumberFormat.getCurrencyInstance( ctx.messages.lang.locale )
        .asInstanceOf[DecimalFormat]
      val dcs = currFmt.getDecimalFormatSymbols
      currFmt.setCurrency( mprice.currency.toJavaCurrency )
      dcs.setCurrencySymbol("")
      dcs.setGroupingSeparator(NBSP)
      currFmt.setDecimalFormatSymbols(dcs)
      currFmt.setGroupingUsed( mprice.amount >= NUMBER_GROUPING_THRESHOLD )
      // Рендерить 99.31, 100.5, 5421 без копеек.
      currFmt.setMaximumFractionDigits(
        if (mprice.amount < 100) 2
        else if (mprice.amount < 1000) 1
        else 0
      )
      currFmt
    }
    val formatted = formatPriceDigitsDF.format(mprice.amount)
      .trim
    pricePostprocess(formatted)
  }


  /** Отформатировать валюту. */
  def formatCurrency(currency: MCurrency)(implicit ctx: Context): String = {
    // TODO Надо бы дедублицировать это с частями formatPrice()
    val lang = ctx.messages.lang
    currency.currencyCode match {
      case "RUB" if lang.language == "ru" =>
        "р."    // Заменяем "руб." на "р."
      case other =>
        currency.toJavaCurrency.getSymbol(lang.toLocale)
    }
  }


  // Пока локали не работают, используем общий для всех форматтер данных.
  private def pcFmt = {
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


  private def temperatureFmt = new DecimalFormat("+#;(-#)")
  def formatTemperature(t: Float): String = {
    temperatureFmt.format(t)
  }


  /** Лимитирование длины строки слева. Если строка длинее указанного порога,
    * то она будет урезана и в конце появится многоточие. */
  def strLimitLen(str: String, maxLen: Int): String = {
    StringUtil.strLimitLen(str, maxLen, ELLIPSIS)
  }

  /**
   * Лимитирование длины строки, но без обрывания слов на середине.
   * @param str Исходная строка.
   * @param len Желаемая минимальная длина строки.
   * @param hard len является жестким лимитом, который нельзя превосходить.
   * @return Новая строка или та же, если она слишком короткая, чтобы резать.
   */
  def strLimitLenNoTrailingWordPart(str: String, len: Int, hard: Boolean = false): String = {
    val l = str.length
    if (l <= len) {
      str
    } else {
      val step = if (hard) -1 else 1
      @tailrec def tryI(i: Int): Int = {
        if (i >= l) {
          -1
        } else if (i <= 0) {
          0
        } else {
          // TODO Добавить проверку на символы пунктуации.
          val ch = str.charAt(i)
          if ( Character.isWhitespace(ch) && (!hard || i < len) ) {
            i
          } else {
            tryI(i + step)
          }
        }
      }
      val i1 = tryI(len)
      if (i1 < 0) {
        str
      } else if (i1 == 0) {
        ""
      } else {
        str.substring(0, i1) + ELLIPSIS
      }
    }
  }


  private val numericDateFormat = DateTimeFormat.forPattern("dd.MM.yyyy")
  def numericDate(dt: ReadableInstant) = numericDateFormat.print(dt)
  def numericDate(d: ReadablePartial)  = numericDateFormat.print(d)

  private val w3cDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  def w3cDate(d: ReadablePartial) = w3cDateFormat.print(d)
  def w3cDate(dt: ReadableInstant) = w3cDateFormat.print(dt)

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

  implicit val tdRadialFc = FieldConstructor(tdRadialFcTpl.f)

  implicit val authFc = FieldConstructor(authFcTpl.f)

  implicit def langSelectFc = FieldConstructor(langSelectFcTpl.f)

}
