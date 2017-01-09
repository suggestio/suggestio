package io.suggest.ym

import scala.util.parsing.combinator._
import org.joda.time._
import parsers.ParserUtil.str2IntF

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.01.14 23:54
 * Description: Набор парсеров, используемых для восприятия значений в форматах, используемых в YML.
 * Парсеры даты-времени написаны на основе [[http://help.yandex.ru/partnermarket/export/date-format.xml этой доки]].
 */
class YmParsers extends JavaTokenParsers {

  /** Регэксп суффикса, который используется и для датированных периодов (месяцы), и для временнЫх (минуты). */
  private def mmRe: Parser[String] = "[MmМм]".r

  import PeriodUnits.{T => PeriodUnit}

  /** Парсер части периода, который описывает календарное исчисление. Вынесен за скобки для упрощения тестирования. */
  def ISO_PERIOD_DATE_PARSER: Parser[List[Int ~ PeriodUnit]] = {
    // Календарный период
    val yearsParser   = ("\\d{1,2}".r ^^ str2IntF) ~ ("[Yy]".r ^^^ PeriodUnits.Year)
    val d13p = "\\d{1,3}".r ^^ str2IntF
    val monthsParser  = d13p ~ (mmRe     ^^^ PeriodUnits.Month)
    val weekParser    = d13p ~ ("[Ww]".r ^^^ PeriodUnits.Week)
    val daysParser    = ("\\d{1,4}".r ^^ str2IntF) ~ ("[Dd]".r ^^^ PeriodUnits.Day)
    rep(yearsParser | monthsParser | weekParser | daysParser)
  }

  /** Парсер части периода, описывающего время (после T). Вынесен за скобки для облегчения тестирования. */
  def ISO_PERIOD_TIME_PARSER: Parser[List[Int ~ PeriodUnit]] = {
    val hoursParser   = ("\\d{1,6}".r ^^ str2IntF) ~ ("[HhНн]".r ^^^ PeriodUnits.hour)
    val minutesParser = ("\\d{1,9}".r ^^ str2IntF) ~ (mmRe       ^^^ PeriodUnits.minute)
    val secondsParser = ("\\d{1,9}".r ^^ str2IntF) ~ ("[Ss]".r   ^^^ PeriodUnits.second)
    rep(hoursParser | minutesParser | secondsParser)
  }

  /** Парсер периода времени в формате iso8601. Используется, например, для описания гарантии на товар. */
  def ISO_PERIOD_PARSER: Parser[Period] = {
    val dtDelim = "[TtТт]".r
    // Время может быть не задано вообще, или же после "T" ничего не задано.
    val iptp = ISO_PERIOD_TIME_PARSER
    val timePeriodOptParser = opt(dtDelim ~> iptp) ^^ { _.getOrElse(Nil) }
    // Собираем период в кучу
    val periodHead: Parser[String] = "[PpРр]".r
    val parser = (periodHead ~> ISO_PERIOD_DATE_PARSER ~ timePeriodOptParser) ^^ {
      case datePeriods ~ timePeriods =>
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
    parser
  }


  /** Парсер обычных булевых значений вида true/false из строк. */
  def PLAIN_BOOL_PARSER: Parser[Boolean] = {
    val boolRe = "(?i)(true|false)".r
    boolRe ^^ { _.toBoolean }
  }

  /** true/false можно задавать через 1/0, on/off и т.д. Тут комплексный парсер булевых значений. */
  def BOOL_PARSER: Parser[Boolean] = {
    val enTrue = "(?i)o[nk]".r | "(?i)yes".r
    val enFalse = "(?i)off+".r | "(?i)no+([np]e)?".r
    val ruTrue: Parser[String] = "(?iu)(есть|да|в\\s*наличи[ие]|ист[ие]на)".r
    val ruFalse: Parser[String] = "(?iu)(отсу[тсц]{1,3}вует|нету?|лож[ьъ]?)".r
    val digiTrue: Parser[String] = "0*[1-9]\\d*".r
    val digiFalse: Parser[String] = "0+".r
    val trueParser   = (enTrue | digiTrue | "+" | ruTrue) ^^^ true
    val falseParser  = (enFalse | digiFalse | "-" | ruFalse) ^^^ false
    PLAIN_BOOL_PARSER | trueParser | falseParser
  }



  // Дата-время. В YM много форматов даты-времени, но все они строго числовые со строгим кол-вом знаков.
  // Поддерживаются даты, даты со временем и даты со временем в таймзоне.

  /** Парсер даты. Описан отдельно для облегчения тестирования. */
  def NUMERIC_DATE_PARSER: Parser[LocalDate] = {
    import io.suggest.util.DateParseUtil._
    val yearParser = RE_YEAR4.r ^^ str2IntF
    val monthParser = "(1[0-2]|0[1-9])".r ^^ str2IntF
    val dayParser = RE_DAY2.r ^^ str2IntF
    val dsep = opt("[-/.]".r)
    val dateYmdParser = yearParser ~ (dsep ~> monthParser) ~ (dsep ~> dayParser) ^^ {
      case yyyy~mm~dd => new LocalDate(yyyy, mm, dd)
    }
    val dateDmyParser = dayParser ~ (dsep ~> monthParser) ~ (dsep ~> yearParser) ^^ {
      case dd~mm~yyyy => new LocalDate(yyyy, mm, dd)
    }
    dateYmdParser | dateDmyParser
  }

  private def SIXTY_PARSER = "[0-5][0-9]".r ^^ str2IntF

  /** Парсер времени. Опциональные поля - это секунды и tz. */
  def TIME_PARSER: Parser[LocalTime] = {
    val hoursParser = "(([0-1][0-9])|2[0-3])".r ^^ str2IntF
    val minutesParser = SIXTY_PARSER
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
        val lt = tzOpt match {
          case Some(tz) => new LocalTime(tz).withHourOfDay(hh).withMinuteOfHour(mm).withSecondOfMinute(ss)
          case None     => new LocalTime(hh, mm, ss)
        }
        lt.withMillisOfSecond(0)
    }
  }

  /** Парсер даты-времени в произвольном формате, описанном в
    * [[http://help.yandex.ru/partnermarket/export/date-format.xml документации]]. */
  def DT_PARSER: Parser[DateTime] = {
    NUMERIC_DATE_PARSER ~ opt(opt("[TtТт]".r) ~> TIME_PARSER) ^^ {
      case date ~ None        => date.toDateTimeAtStartOfDay
      case date ~ Some(time)  => date.toDateTime(time)
    }
  }

}

/** Внутренние единицы периода. Используются в промежуточных результатах парсинга из-за отсутствия fold-парсинга. */
object PeriodUnits extends Enumeration {
  type T = Value
  val Year, Month, Week, Day, hour, minute, second = Value
}


