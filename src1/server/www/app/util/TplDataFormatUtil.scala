package util

import java.text.{DecimalFormat, NumberFormat}
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Locale

import io.suggest.bill._
import io.suggest.common.html.HtmlConstants
import io.suggest.common.html.HtmlConstants.ELLIPSIS
import io.suggest.geo._
import io.suggest.i18n.MsgCodes
import io.suggest.text.StringUtil
import io.suggest.xplay.json.PlayJsonUtil
import models.mctx.Context
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

  import HtmlConstants.NBSP

  def NUMBER_GROUPING_THRESHOLD = 10000

  /** Постпроцессинг цен. Использовать неразрывные пробелы вместо обычных. */
  def pricePostprocess(priceStr: String): String = {
    priceStr.replace('\u0020', NBSP)
  }

  def formatPrice(price: IPrice)(implicit ctx: Context): String = {
    val currFmt = NumberFormat.getCurrencyInstance( ctx.messages.lang.locale )
      .asInstanceOf[DecimalFormat]
    currFmt.setCurrency(price.currency.toJavaCurrency)
    val currencySymbol = formatCurrency(price.currency)
    val dcs = currFmt.getDecimalFormatSymbols
    dcs.setCurrencySymbol(currencySymbol)
    dcs.setGroupingSeparator(NBSP)
    // TODO выставить остальные сепараторы, чтобы не вызывать pricePostprocess()
    currFmt.setDecimalFormatSymbols(dcs)
    val realAmount = price.realAmount
    currFmt.setGroupingUsed(realAmount >= NUMBER_GROUPING_THRESHOLD)
    val formatted = currFmt.format(realAmount)
    pricePostprocess(formatted)
  }

  /** Напечатать цену согласно локали и валюте. */
  def formatPrice(price: Amount_t, currency: MCurrency)(implicit ctx: Context): String = {
    formatPrice( MPrice(price, currency) )
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
    * Выставить в mprice отформатированное значение amount'а.
    * @param mprice Цена.
    * @param ctx Контекст рендера.
    * @return Обновлённый инстанс MPrice.
    */
  def setFormatPrice(mprice: MPrice)(implicit ctx: Context): MPrice = {
    mprice.withAmountStrOpt(
      Some(
        formatPriceAmount(mprice)
      )
    )
  }

  /**
    * Форматирование amount стоимости в строку БЕЗ символа валюты.
    * @param mprice Сумма.
    * @param ctx Контекст рендера.
    * @return Строка вида "10 034"
    */
  def formatPriceAmount(mprice: IPrice)(implicit ctx: Context): String = {
    val realAmount = mprice.realAmount
    formatPriceRealAmount(realAmount, mprice.currency)
  }
  def formatPriceRealAmount(realAmount: Double, currency: MCurrency)(implicit ctx: Context): String = {
    val formatPriceDigitsDF = {
      val currFmt = NumberFormat.getCurrencyInstance( ctx.messages.lang.locale )
        .asInstanceOf[DecimalFormat]
      currFmt.setCurrency( currency.toJavaCurrency )

      val dcs = currFmt.getDecimalFormatSymbols
      dcs.setCurrencySymbol("")
      dcs.setGroupingSeparator(NBSP)
      currFmt.setDecimalFormatSymbols(dcs)

      currFmt.setGroupingUsed( realAmount >= NUMBER_GROUPING_THRESHOLD )
      // Рендерить 99.31, 100.5, 5421 без копеек.
      currFmt.setMaximumFractionDigits(
        if (realAmount < 1000) 2
        else if (realAmount < 10000) 1
        else 0
      )
      currFmt
    }
    val formatted = formatPriceDigitsDF
      .format(realAmount)
      .trim
    pricePostprocess(formatted)
  }


  /** Отрендерить amount цены в сухом формате. */
  def priceAmountPlainFmt(mcurrency: MCurrency): DecimalFormat = {
    val currFmt = NumberFormat.getNumberInstance( Locale.ROOT )
      .asInstanceOf[DecimalFormat]
    currFmt.setDecimalSeparatorAlwaysShown(true)

    val dcs = currFmt.getDecimalFormatSymbols
    dcs.setDecimalSeparator('.')
    currFmt.setDecimalFormatSymbols(dcs)

    val frac = mcurrency.exponent
    currFmt.setMaximumFractionDigits( frac )
    currFmt.setMinimumFractionDigits( frac )
    currFmt.setGroupingUsed(false)
    currFmt
  }
  def formatPriceAmountPlain(mprice: IPrice): String = {
    priceAmountPlainFmt(mprice.currency)
      .format(mprice.realAmount)
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


  private val numericDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")
  def numericDate(dt: TemporalAccessor) = numericDateFormat.format(dt)

  private val w3cDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  def w3cDate(d: TemporalAccessor) = w3cDateFormat.format(d)

  private val numericDtFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
  // TODO Нужно прикрутить поддержку timezone, используя context
  def numericDt(dt: TemporalAccessor)(implicit ctx: Context) = numericDtFormat.format(dt)

  private val numericTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
  def numericTime(dt: TemporalAccessor)(implicit ctx: Context) = numericTimeFormat.format(dt)


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


  def formatCoords(mgp: MGeoPoint)(implicit ctx: Context): String = {
    mgp.toHumanFriendlyString
  }

  def formatDistance(d: Distance)(implicit ctx: Context): String = {
    val msg = DistanceUtil.formatDistanceM( d.meters )
    ctx.messages(
      msg.message,
      PlayJsonUtil.fromJsArray( msg.args ): _*,
    )
  }

  /** Отформатировать GeoShape в некоторую строку. */
  def formatGeoShape(gs: IGeoShape)(implicit ctx: Context): String = {
    gs match {
      // Круг описывается точкой и радиусом. Используется в георазмещении карточек.
      case circle: CircleGs =>
        ctx.messages(
          MsgCodes.`in.radius.of.0.from.1`,
          formatDistance( CircleGsJvm.distance(circle) ),
          formatCoords(circle.center)
        )

      // Размещение в точке. Используется в lk-adn-map.
      case point: PointGs =>
        ctx.messages("in.geopoint.x", formatCoords(point.coord))

      // Другие шейпы. Изначально их толком и не было, поэтому рендерим кое-как.
      case _ =>
        val nearStr = ctx.messages("near")
        val coordStr = formatCoords( gs.centerPoint.getOrElse(gs.firstPoint) )
        s"$nearStr $coordStr"
    }
  }

}


/** Различные field constructor'ы. */
object FC {

  implicit def tdFc = FieldConstructor(tdFcTpl.f)

  implicit def divFc = FieldConstructor(divFcTpl.f)

  implicit def checkboxFc = FieldConstructor(checkboxFcTpl.f)

  implicit def radialFc = FieldConstructor(radialFcTpl.f)

  implicit def tdRadialFc = FieldConstructor(tdRadialFcTpl.f)

  implicit def authFc = FieldConstructor(authFcTpl.f)

  implicit def langSelectFc = FieldConstructor(langSelectFcTpl.f)

}
