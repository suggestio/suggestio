package util

import org.joda.time.format.{PeriodFormatter, DateTimeFormatterBuilder, PeriodFormatterBuilder}
import org.joda.time.{DateTimeZone, Duration, Period, DateTime}
import java.util.Locale

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 13:35
 * Description: Вывод писанины в стиле "1 day ago"
 */

object DateTimePrettyPrinter {

  // Потом локаль можно вынести из модуля

  private val locale = new Locale("ru", "RU")

  private val seconds_ms = 1000
  private val minute = 60 * seconds_ms
  private val hour = minute * 60
  private val day  = hour * 24

  private val dur_10sec  = new Duration(10 * seconds_ms)
  private val dur_minute = new Duration(minute)
  private val dur_hour   = new Duration(hour)
  private val dur_6hours = new Duration(6 * hour)
  private val dur_2days  = new Duration(2 * day)

  private type Formatters_t = (PeriodFormatter, PeriodFormatter, PeriodFormatter)

  // Потом следует по geoIP определять таймзону (нужно портировать из Alterrussia).
  private val peerTz = DateTimeZone.forID("Europe/Moscow")

  /**
   * Генератор форматтеров для минут (минута, минуты, ...).
   * @param mSuffix окончание для слова "минут"
   * @return
   */
  private def minutesFormatterRu(mSuffix:String, isFuture:Boolean) = {
    val suffix = " минут" + mSuffix + " назад"
    new PeriodFormatterBuilder()
      .appendMinutes.appendSuffix(suffix)
      .printZeroNever
      .toFormatter
      .withLocale(locale)
  }

  private def hoursFormatterRu(hSuffix:String, isFuture:Boolean) = {
    val builder = new PeriodFormatterBuilder()
    if (isFuture) {
      builder.appendPrefix("Через ").appendHours().appendSuffix(" час" + hSuffix)
    } else {
      builder.appendHours().appendSuffix(" час" + hSuffix + " назад")
    }
    builder.printZeroNever()
      .toFormatter
      .withLocale(locale)
  }


  private val minutesPastFormatters   = minutesFormatters(isFuture = false)
  private val hoursPastFormatters     = hourFormatters(isFuture = false)
  private val minutesFutureFormatters = minutesFormatters(isFuture = true)
  private val hoursFutureFormatters   = hourFormatters(isFuture = true)


  private def hourFormatters(isFuture:Boolean) : Formatters_t = {
    val hf = hoursFormatterRu(_:String, isFuture)
    (hf(""), hf("а"), hf("ов"))
  }

  private def minutesFormatters(isFuture:Boolean) : Formatters_t = {
    val mf = minutesFormatterRu(_:String, isFuture)
    (mf("у"), mf("ы"), mf(""))
  }


  /**
   * Отформатировать число минут, выбрав нужный форматтер в зависимости от числа минут.
   * @param period Временной промежуток. Считается, что он менее часа.
   * @return Строка вида "2 минуты назад".
   */
  private def formatSomeRu(period:Period, units:Int, formatters:Formatters_t): String = {
    val (f1, fSeveral, fMany) = formatters
    // В зависимости от кол-ва минут надо выбрать тот или иной форматтер.
    val formatter = if (units >= 10 && units < 20)
      fMany
    else {
      val m = units % 10
      if (m == 1)
        f1
      else if (m >= 2 && m <= 4)
        fSeveral
      else
        fMany
    }
    formatter.print(period)
  }


  private val dtFormatterRecent = new DateTimeFormatterBuilder()
    .appendDayOfMonth(1).appendLiteral(' ')
    .appendMonthOfYearShortText().appendLiteral(' ')
    .appendYear(4, 4).appendLiteral(' ')
    .appendHourOfDay(1).appendLiteral(':')
    .appendMinuteOfHour(2)
    .toFormatter
    .withLocale(locale)

  private val dtFormatterOld = new DateTimeFormatterBuilder()
    .appendDayOfMonth(1).appendLiteral(' ')
    .appendMonthOfYearShortText().appendLiteral(' ')
    .appendYear(4, 4)
    .toFormatter
    .withLocale(locale)

  /**
   * Выполнить форматирование
   * @param dt Указанное время
   * @return Человеческое время.
   */
  def humanizeDt(dt:DateTime, isCapitalized:Boolean = true)(implicit ctx: Context) : String = {
    val now = ctx.now
    val isFuture = dt.isAfter(now)
    val d = if (isFuture)
      new Duration(now, dt)
    else
      new Duration(dt, now)
    // Сгенерить подходящую строку.
    val result = if (d.isShorterThan(dur_10sec)) {
      if (isFuture)
        "Через несколько секунд"
      else
        "Несколько секунд назад"

    } else if (d.isShorterThan(dur_minute)) {
      if (isFuture)
        "Менее чем через минуту"
      else
        "Меньше минуты назад"

    } else if (d.isShorterThan(dur_hour)) {
      val period = d.toPeriodTo(now)
      val formatters = if (isFuture)
        minutesFutureFormatters
      else
        minutesPastFormatters
      formatSomeRu(period, period.getMinutes, formatters)

    } else if (d.isShorterThan(dur_6hours)) {
      val period = d.toPeriodTo(now)
      val formatters = if (isFuture)
        hoursFutureFormatters
      else
        hoursPastFormatters
      formatSomeRu(period, period.getHours, formatters)

    // Далее, выводим даты напрямую. Нужно из свигать по таймзоне.
    } else if (d.isShorterThan(dur_2days)) {
      dtFormatterRecent.print(dt.withZone(peerTz))

    } else {
      dtFormatterOld.print(dt.withZone(peerTz))
    }
    // Обработать параметр капитализации.
    if (isCapitalized) {
      result
    } else {
      result.head.toLower + result.tail
    }
  }


  def formatDate(dt: DateTime)(implicit ctx: Context): String = {
    dtFormatterOld.print(dt)
  }
}
