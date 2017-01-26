package util

import java.time.ZoneId

import models.mctx.Context
import org.joda.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, PeriodFormatter, PeriodFormatterBuilder}
import org.joda.time._
import java.util.Locale

// FIXME Тут просто адовый быдлокод с очень древних времён.
// TODO Нужно переписать всё на Messages, выкинув всякие PeriodFormatters, т.к. в java8 их как бы нет.
// TODO Выкинуть joda-time отсюда.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.04.13 13:35
 * Description: Вывод писанины в стиле "1 day ago"
 */

object DateTimePrettyPrinter {

  // Потом локаль можно вынести из модуля

  private val LOCALE_RUSSIAN = new Locale("ru", "RU")

  private val dur_10sec  = Duration.standardSeconds(10)
  private val dur_minute = Duration.standardMinutes(1)
  private val dur_hour   = Duration.standardHours(1)
  private val dur_6hours = Duration.standardHours(6)
  private val dur_2days  = Duration.standardDays(2)

  private type Formatters_t = (PeriodFormatter, PeriodFormatter, PeriodFormatter)

  // Потом следует по geoIP определять таймзону (нужно портировать из Alterrussia).
  private val peerTz = DateTimeZone.forID("Europe/Moscow")


  // TODO i18n Нужна локализация тут через ctx.messages:
  /**
   * Генератор форматтеров для минут (минута, минуты, ...).
   * @param mSuffix окончание для слова "минут"
   * @return
   */
  private def minutesFormatterRu(mSuffix: String, isFuture: Boolean) = {
    val suffix = " минут" + mSuffix + " назад"
    new PeriodFormatterBuilder()
      .appendMinutes.appendSuffix(suffix)
      .printZeroNever
      .toFormatter
      .withLocale(LOCALE_RUSSIAN)
  }

  // TODO i18n Нужна локализация тут через ctx.messages:
  private def hoursFormatterRu(hSuffix: String, isFuture: Boolean) = {
    val builder = new PeriodFormatterBuilder()
    if (isFuture) {
      builder.appendPrefix("Через ").appendHours().appendSuffix(" час" + hSuffix)
    } else {
      builder.appendHours().appendSuffix(" час" + hSuffix + " назад")
    }
    builder.printZeroNever()
      .toFormatter
      .withLocale(LOCALE_RUSSIAN)
  }


  private val minutesPastFormatters   = minutesFormatters(isFuture = false)
  private val hoursPastFormatters     = hourFormatters(isFuture = false)
  private val minutesFutureFormatters = minutesFormatters(isFuture = true)
  private val hoursFutureFormatters   = hourFormatters(isFuture = true)


  // TODO i18n Нужна локализация окончаний тут через ctx.messages:
  private def hourFormatters(isFuture: Boolean) : Formatters_t = {
    val hf = hoursFormatterRu(_: String, isFuture)
    (hf(""), hf("а"), hf("ов"))
  }

  // TODO i18n Нужна локализация окончаний тут через ctx.messages:
  private def minutesFormatters(isFuture:Boolean) : Formatters_t = {
    val mf = minutesFormatterRu(_: String, isFuture)
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
    .withLocale(LOCALE_RUSSIAN)

  private val dtFormatterOld = new DateTimeFormatterBuilder()
    .appendDayOfMonth(1).appendLiteral(' ')
    .appendMonthOfYearShortText().appendLiteral(' ')
    .appendYear(4, 4)
    .toFormatter
    .withLocale(LOCALE_RUSSIAN)

  private val dateFormatterFull = new DateTimeFormatterBuilder()
    .appendDayOfMonth(1).appendLiteral(' ')
    .appendMonthOfYearText().appendLiteral(' ')
    .appendYear(4, 4)
    .toFormatter
    .withLocale(LOCALE_RUSSIAN)

  /** "пятница". */
  val dayOfWeekFmt = {
    new DateTimeFormatterBuilder()
      .appendDayOfWeekText()
      .toFormatter
  }

  object MsgWords {

    def SEVERAL_SECONDS   = "Several.seconds."
    def LESS_THAN_MINUTE  = "Less.than.minute."
    def LATER             = "later"
    def AGO               = "ago"

    def laterOrAgo(isFuture: Boolean): String = {
      if (isFuture) LATER else AGO
    }
  }

  def toJodaTime(dt8: java.time.OffsetDateTime): DateTime = {
    new DateTime( dt8.toInstant.toEpochMilli )
  }
  def toJodaTime(dt8: java.time.ZonedDateTime): DateTime = {
    toJodaTime( dt8.toOffsetDateTime )
  }

  /**
   * Выполнить форматирование даты относительно "сейчас", опустив незначительные детали.
   * @param dt Указанное время
   * @return Человеческое время.
   */
  def humanizeDt(dt: java.time.OffsetDateTime, isCapitalized: Boolean)(implicit ctx: Context) : String = {
    humanizeDt(toJodaTime(dt), isCapitalized)
  }
  def humanizeDt(dt: DateTime, isCapitalized: Boolean)(implicit ctx: Context) : String = {
    import ctx.messages

    val now = toJodaTime( ctx.now )

    val isFuture = dt.isAfter(now)
    val d = if (isFuture)
      new Duration(now, dt)
    else
      new Duration(dt, now)

    // Сгенерить подходящую строку.
    val result = if (d.isShorterThan(dur_10sec)) {
      import MsgWords._
      messages(SEVERAL_SECONDS + laterOrAgo(isFuture))

    } else if (d.isShorterThan(dur_minute)) {
      import MsgWords._
      messages(LESS_THAN_MINUTE + laterOrAgo(isFuture))

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
      // TODO Opt Большинство отображаемых дат в далёком прошлом, может эту ветку else вынести в начало if? Или if вывернуть наизнанку?
      dtFormatterOld.print(dt.withZone(peerTz))
    }
    // Обработать параметр капитализации.
    if (isCapitalized) {
      result
    } else {
      result.head.toLower + result.tail
    }
  }


  /** 21 янв 2015 */
  // TODO выставить здесь LocalDate вместо DateTime.
  def formatDate(dt: DateTime)(implicit ctx: Context): String = {
    _prepareFormatter(dtFormatterOld)
      .print(dt)
  }
  def formatDate(dt: ReadablePartial)(implicit ctx: Context): String = {
    _formatPartialWith(dt, dtFormatterOld)
  }

  /** 21 января 2015 */
  def formatDateFull(d: java.time.LocalDate)(implicit ctx: Context): String = {
    val jld = new LocalDate(d.getYear, d.getMonthValue, d.getDayOfMonth)
    formatDateFull(jld)
  }
  def formatDateFull(d: LocalDate)(implicit ctx: Context): String = {
    _formatPartialWith(d, dateFormatterFull)
  }

  /** пятница, friday. */
  def dayOfWeek(d: java.time.LocalDate)(implicit ctx: Context): String = {
    val jld = new LocalDate( d.getYear, d.getMonthValue, d.getDayOfMonth )
    dayOfWeek(jld)
  }
  def dayOfWeek(d: LocalDate)(implicit ctx: Context): String = {
    _formatPartialWith(d, dayOfWeekFmt)
  }

  private def _prepareFormatter(fmt: DateTimeFormatter)(implicit ctx: Context): DateTimeFormatter = {
    fmt.withLocale( ctx.messages.lang.toLocale )
  }
  /** Дедубликация кода использования DT-formatter'а. */
  private def _formatPartialWith(rp: ReadablePartial, fmt: DateTimeFormatter)(implicit ctx: Context): String = {
    _prepareFormatter(fmt)
      .print(rp)
  }

  private val yyyyMMddFmt = new DateTimeFormatterBuilder()
    .appendYear(4, 4).appendLiteral('-')
    .appendMonthOfYear(2).appendLiteral('-')
    .appendDayOfMonth(2)
    .toFormatter
  def formatDateDeficedYYYYmmDD(dt: DateTime): String = {
    yyyyMMddFmt.print(dt)
  }
  def formatDateDeficedYYYYmmDD(d: LocalDate): String = {
    yyyyMMddFmt.print(d)
  }

}
