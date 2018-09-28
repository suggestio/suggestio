package io.suggest.dt

import java.time.format.DateTimeFormatter
import java.time.OffsetDateTime
import java.util.Locale

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.05.13 15:11
 * Description: Серверная утиль для работы с датами/временем.
 */
object DateTimeUtil {

  // Обработка rfc-дат для реквестов и запросов.

  /** Форматирование RFC даты, пригодную для отправки/получения внутри http-заголовка. */
  def rfcDtFmt: DateTimeFormatter = {
    DateTimeFormatter.RFC_1123_DATE_TIME
  }

  /** Простое челове-читабельное форматирование значения LocalDate. */
  val simpleLocalDateFmt: DateTimeFormatter = {
    DateTimeFormatter
      .ofPattern( "dd MMM yyyy" )
      .withLocale(Locale.getDefault)
  }

  def parseRfcDate(date: String): Option[OffsetDateTime] = {
    try {
      // Тут GMT резалось из-за каких-то проблем с joda-time. Сейчас всё упрощено, и должно бы работать.
      val d = OffsetDateTime.parse(date, rfcDtFmt)
      Some(d)

    } catch {
      case _:Exception => None
    }
  }

}
