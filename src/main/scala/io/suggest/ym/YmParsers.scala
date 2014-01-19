package io.suggest.ym

import scala.util.parsing.combinator._
import org.joda.time._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.14 23:54
 * Description: Набор парсеров, используемых для восприятия значений в форматах, используемых в YML.
 * Парсеры даты-времени написаны на основе [[http://help.yandex.ru/partnermarket/export/date-format.xml этой доки]].
 */
object YmParsers extends JavaTokenParsers {

  /** Функция перегона строки в целое. Используется в date/time-парсерах. */
  private val str2IntF = {s: String => s.toInt }

  /** Функция перегона строки в число с плавающей точкой. */
  private val str2FloatF = {s: String => s.toFloat }
  
  
  /** Парсер измерений (размерности) товара в формате "длина/ширина/высота". */
  val DIMENSIONS_PARSER = {
    val sepParser: Parser[String] = "/"
    val dn = decimalNumber ^^ str2FloatF
    dn ~ (sepParser ~> dn) ~ (sepParser ~> dn) ^^ {
      case ls~ws~hs => Dimensions(ls, ws, hs)
    }
  }

  /** Парсер периода времени в формате iso8601. Используется, например, для описания гарантии на товар. */
  val ISO_TIMEPERIOD_PARSER: Parser[Period] = {
    // Дата
    val mmRe: Parser[String] = "[MmМм]".r
    val yearsParser   = ("\\d{1,2}".r ^^ str2IntF) ~ ("[Yy]".r ^^^ PeriodUnits.Year)
    val monthsParser  = ("\\d{1,3}".r ^^ str2IntF) ~ (mmRe ^^^ PeriodUnits.Month)
    val weekParser    = ("\\d{1,3}".r ^^ str2IntF) ~ ("[Ww]".r ^^^ PeriodUnits.Week)
    val daysParser    = ("\\d{1,4}".r ^^ str2IntF) ~ ("[Dd]".r ^^^ PeriodUnits.Day)
    val datePeriodParser = rep(yearsParser | monthsParser | weekParser | daysParser)
    // время
    val hoursParser   = ("\\d{1,6}".r ^^ str2IntF) ~ ("[HhНн]".r ^^^ PeriodUnits.hour)
    val minutesParser = ("\\d{1,9}".r ^^ str2IntF) ~ (mmRe ^^^ PeriodUnits.minute)
    val secondsParser = ("\\d{1,9}".r ^^ str2IntF) ~ ("[Ss]".r ^^^ PeriodUnits.second)
    val timePeriodParser = rep(hoursParser | minutesParser | secondsParser)
    val dtDelim = "[TtТт]".r
    // Время может быть не задано вообще, или же после "T" ничего не задано.
    val timePeriodOptParser = opt(dtDelim ~> opt(timePeriodParser)) ^^ { _.flatten getOrElse Nil }
    // Собираем период в кучу
    val periodHead: Parser[String] = "[PpРр]"
    periodHead ~> datePeriodParser ~ timePeriodOptParser ^^ { case datePeriods ~ timePeriods =>
      (datePeriods ++ timePeriods).foldLeft(new Period) {
        case (acc0, y ~ PeriodUnits.Year)   => acc0.withYears(y)
        case (acc0, m ~ PeriodUnits.Month)  => acc0.withMonths(m)
        case (acc0, w ~ PeriodUnits.Week)   => acc0.withWeeks(w)
        case (acc0, d ~ PeriodUnits.Day)    => acc0.withDays(d)
        case (acc0, h ~ PeriodUnits.hour)   => acc0.withHours(h)
        case (acc0, m ~ PeriodUnits.minute) => acc0.withMinutes(m)
        case (acc0, s ~ PeriodUnits.second) => acc0.withSeconds(s)
      }
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


  // Дата-время. В YM много форматов даты-времени, но все они строго числовые со строгим кол-вом знаков.
  // Поддерживаются даты, даты со временем и даты со временем в таймзоне.

  /** Парсер даты. Описан отдельно для облегчения тестирования. */
  val NUMERIC_DATE_PARSER: Parser[LocalDate] = {
    import io.suggest.util.DateParseUtil._
    val yearParser = RE_YEAR4 ^^ str2IntF
    val monthParser = RE_MONTH_I2 ^^ str2IntF
    val dayParser = RE_DAY2 ^^ str2IntF
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
    val hoursParser = "(([0-1][0-9])|2[0-3])".r ^^ str2IntF
    val minutesParser = "[0-5][0-9]" ^^ str2IntF
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


object PeriodUnits extends Enumeration {
  type PeriodUnit = Value
  val Year, Month, Week, Day, hour, minute, second = Value
}
