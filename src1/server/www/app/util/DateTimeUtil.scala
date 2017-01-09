package util

import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import java.util.Locale

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 15:11
 * Description:
 */
object DateTimeUtil {

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)

  // Обработка rfc-дат для реквестов и запросов.

  private val timeZoneCode = "GMT"

  /** Форматирование RFC даты, пригодную для отправки/получения внутри http-заголовка. */
  val rfcDtFmt: DateTimeFormatter = {
    DateTimeFormat
      .forPattern("EEE, dd MMM yyyy HH:mm:ss '" + timeZoneCode + "'")
      .withLocale(java.util.Locale.ENGLISH)
      .withZone(DateTimeZone.forID(timeZoneCode))
  }

  /** Аналог rfcDtFmt, но без таймзоны. */
  val rfcDtNoTzFmt: DateTimeFormatter = {
    DateTimeFormat
      .forPattern("EEE, dd MMM yyyy HH:mm:ss")
      .withLocale(java.util.Locale.ENGLISH)
      .withZone(DateTimeZone.forID(timeZoneCode))
  }

  /** Простое челове-читабельное форматирование значения LocalDate. */
  val simpleLocalDateFmt: DateTimeFormatter = {
    DateTimeFormat
      .forPattern("dd MMM yyyy")
      .withLocale(Locale.getDefault)
      .withZone(DateTimeZone.forID(timeZoneCode))
  }

  private val parsableTimezoneCode = " " + timeZoneCode

  def parseRfcDate(date: String): Option[DateTime] = {
    try {
      //jodatime does not parse timezones, so we handle that manually
      val d = rfcDtNoTzFmt.parseDateTime(date.replace(parsableTimezoneCode, ""))
      Some(d)

    } catch {
      case _:Exception => None
    }
  }

}
