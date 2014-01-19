package io.suggest.ym

import scala.util.parsing.combinator._
import org.joda.time._
import org.joda.time.format._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.14 23:54
 * Description: Набор парсеров, используемых для восприятия значений в форматах, используемых в YML.
 * Парсеры даты-времени написаны на основе [[http://help.yandex.ru/partnermarket/export/date-format.xml этой доки]].
 */
object YmParsers extends JavaTokenParsers {

  /** Парсер измерений (размерности) товара в формате "длина/ширина/высота". */
  val DIMENSIONS_PARSER = {
    val sepParser: Parser[String] = "/"
    val dn = decimalNumber ^^ { _.toFloat }
    dn ~ (sepParser ~> dn) ~ (sepParser ~> dn) ^^ {
      case ls ~ ws ~ hs  =>  Dimensions(ls, ws, hs)
    }
  }

  /** Парсер периода времени в формате iso8601. Используется, например, для описания гарантии на товар. */
  val ISO_TIMEPERIOD_PARSER: Parser[Period] = {
    val isoRe = "(?i)[P]([0-9.]+[YMWDHMST]){1,}".r
    isoRe ^^ {
      iso8601 => Period.parse(iso8601.toUpperCase, ISOPeriodFormat.standard)
    }
  }


  /** Парсер обычных булевых значений вида true/false из строк. */
  val PLAIN_BOOL_PARSER: Parser[Boolean] = {
    val boolRe = "(?i)(true|false)".r
    boolRe ^^ { _.toBoolean }
  }


  /** Для парсинга гарантии применяется комбинация из boolean-парсера и парсера периода времени. */
  val WARRANTY_PARSER: Parser[Warranty] = {
    val bp = PLAIN_BOOL_PARSER ^^ { Warranty(_) }
    val pp = ISO_TIMEPERIOD_PARSER ^^ {
      period => Warranty(hasWarranty=true, warrantyPeriod=Some(period))
    }
    bp | pp
  }


  // Функция перегона строки в целое. Используется в date/time-парсерах.
  private val toIntF = {s: String => s.toInt }

  // Дата-время. В YM много форматов даты-времени, но все они строго числовые со строгим кол-вом знаков.
  // Поддерживаются даты, даты со временем и даты со временем в таймзоне.
  import io.suggest.util.DateParseUtil._

  /** Парсер даты. Описан отдельно для облегчения тестирования. */
  val NUMERIC_DATE_PARSER: Parser[LocalDate] = {
    val yearParser = RE_YEAR4 ^^ toIntF
    val monthParser = RE_MONTH_I2 ^^ toIntF
    val dayParser = RE_DAY2 ^^ toIntF
    val dateTokensDelim = opt("[-/.]".r)
    val dateYmdParser = yearParser ~ (dateTokensDelim ~> monthParser) ~ (dateTokensDelim ~> dayParser) ^^ {
      case yyyy~mm~dd => new LocalDate(yyyy, mm, dd)
    }
    val dateDmyParser = dayParser ~ (dateTokensDelim ~> monthParser) ~ (dateTokensDelim ~> yearParser) ^^ {
      case dd~mm~yyyy => new LocalDate(yyyy, mm, dd)
    }
    dateYmdParser | dateDmyParser
  }

  /** Парсер времени. Опциональные поля - это секунды и tz. */
  val TIME_PARSER: Parser[LocalTime] = {
    val hoursParser = "(([0-1][0-9])|2[0-3])".r ^^ toIntF
    val minutesParser = "[0-5][0-9]" ^^ toIntF
    val secondsParser = minutesParser
    val timeSepParser = opt(":")
    val hourMinutesParser = hoursParser ~ (timeSepParser ~> minutesParser)
    val timeParser = hourMinutesParser ~ opt(timeSepParser ~> secondsParser)
    // Временная зона может быть в разных форматах:
    val tzSepParser = "[+-]".r
    val tzParser = tzSepParser ~ hourMinutesParser ^^ { case tzSign ~ (tzHh ~ tzMm) =>
      val hh = tzSign match {
        case "-" => -tzHh
        case "+" => tzHh
      }
      DateTimeZone.forOffsetHoursMinutes(hh, tzMm)
    }
    timeParser ~ opt(tzParser) ^^ {
      case hh ~ mm ~ ssOpt ~ tzOpt =>
        val ss = ssOpt getOrElse 0
        tzOpt match {
          case Some(tz) => new LocalTime(tz).withHourOfDay(hh).withMinuteOfHour(mm).withSecondOfMinute(ss)
          case None     => new LocalTime(hh, mm, ss)
        }
    }
  }

  /** Парсер даты-времени в произвольном формате, описанном в
    * [[http://help.yandex.ru/partnermarket/export/date-format.xml документации]]. */
  val DT_PARSER: Parser[DateTime] = {
    val dtSep = "(T|\\s+)".r
    NUMERIC_DATE_PARSER ~ opt(dtSep ~> TIME_PARSER) ^^ {
      case date ~ None        => date.toDateTimeAtStartOfDay
      case date ~ Some(time)  => date.toDateTime(time)
    }
  }

}
