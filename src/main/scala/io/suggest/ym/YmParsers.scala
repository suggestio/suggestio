package io.suggest.ym

import scala.util.parsing.combinator._
import org.joda.time._
import model._
import HotelMealTypes.HotelMealType
import HotelStarsLevels.HotelStarsLevel
import io.suggest.ym.ParamNames.ParamName
import YmParsers.PeriodUnits.PeriodUnit

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
    val sepParser: Parser[String] = "[/xх*]".r
    val dn = decimalNumber ^^ str2FloatF
    dn ~ (sepParser ~> dn) ~ (sepParser ~> dn) ^^ {
      case ls~ws~hs => Dimensions(ls, ws, hs)
    }
  }


  /** Регэксп суффикса, который используется и для датированных периодов (месяцы), и для временнЫх (минуты). */
  private val mmRe: Parser[String] = "[MmМм]".r

  /** Парсер части периода, который описывает календарное исчисление. Вынесен за скобки для упрощения тестирования. */
  val ISO_PERIOD_DATE_PARSER: Parser[List[Int ~ PeriodUnit]] = {
    // Календарный период
    val yearsParser   = ("\\d{1,2}".r ^^ str2IntF) ~ ("[Yy]".r ^^^ PeriodUnits.Year)
    val monthsParser  = ("\\d{1,3}".r ^^ str2IntF) ~ (mmRe     ^^^ PeriodUnits.Month)
    val weekParser    = ("\\d{1,3}".r ^^ str2IntF) ~ ("[Ww]".r ^^^ PeriodUnits.Week)
    val daysParser    = ("\\d{1,4}".r ^^ str2IntF) ~ ("[Dd]".r ^^^ PeriodUnits.Day)
    rep(yearsParser | monthsParser | weekParser | daysParser)
  }

  /** Парсер части периода, описывающего время (после T). Вынесен за скобки для облегчения тестирования. */
  val ISO_PERIOD_TIME_PARSER: Parser[List[Int ~ PeriodUnit]] = {
    val hoursParser   = ("\\d{1,6}".r ^^ str2IntF) ~ ("[HhНн]".r ^^^ PeriodUnits.hour)
    val minutesParser = ("\\d{1,9}".r ^^ str2IntF) ~ (mmRe       ^^^ PeriodUnits.minute)
    val secondsParser = ("\\d{1,9}".r ^^ str2IntF) ~ ("[Ss]".r   ^^^ PeriodUnits.second)
    rep(hoursParser | minutesParser | secondsParser)
  }

  /** Парсер периода времени в формате iso8601. Используется, например, для описания гарантии на товар. */
  val ISO_PERIOD_PARSER: Parser[Period] = {
    val dtDelim = "[TtТт]".r
    // Время может быть не задано вообще, или же после "T" ничего не задано.
    val timePeriodOptParser = opt(dtDelim ~> ISO_PERIOD_TIME_PARSER) ^^ { _ getOrElse Nil }
    // Собираем период в кучу
    val periodHead: Parser[String] = "[PpРр]".r
    val parser = periodHead ~> ISO_PERIOD_DATE_PARSER ~ timePeriodOptParser ^^ { case datePeriods ~ timePeriods =>
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
  val PLAIN_BOOL_PARSER: Parser[Boolean] = {
    val boolRe = "(?i)(true|false)".r
    boolRe ^^ { _.toBoolean }
  }

  /** true/false можно задавать через 1/0, on/off и т.д. Тут комплексный парсер булевых значений. */
  val BOOL_PARSER: Parser[Boolean] = {
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


  /** Для парсинга гарантии применяется комбинация из boolean-парсера и парсера периода времени. */
  val WARRANTY_PARSER: Parser[Warranty] = {
    val bp = PLAIN_BOOL_PARSER ^^ {
      case true  => WarrantyNoPeriod
      case false => NoWarranty
    }
    val pp = ISO_PERIOD_PARSER ^^ {
      period => HasWarranty(period)
    }
    bp | pp
  }


  // Дата-время. В YM много форматов даты-времени, но все они строго числовые со строгим кол-вом знаков.
  // Поддерживаются даты, даты со временем и даты со временем в таймзоне.

  /** Парсер даты. Описан отдельно для облегчения тестирования. */
  val NUMERIC_DATE_PARSER: Parser[LocalDate] = {
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

  private val SIXTY_PARSER = "[0-5][0-9]".r ^^ str2IntF

  /** Парсер времени. Опциональные поля - это секунды и tz. */
  val TIME_PARSER: Parser[LocalTime] = {
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
  val DT_PARSER: Parser[DateTime] = {
    NUMERIC_DATE_PARSER ~ opt(opt("[TtТт]".r) ~> TIME_PARSER) ^^ {
      case date ~ None        => date.toDateTimeAtStartOfDay
      case date ~ Some(time)  => date.toDateTime(time)
    }
  }


  val EXPIRY_PARSER: Parser[Either[DateTime, Period]] = {
    val pp = ISO_PERIOD_PARSER ^^ { Right(_) }
    val dp = DT_PARSER ^^ { Left(_) }
    pp | dp
  }

  /** Парсер длительности звучания аудиозаписи. Задаётся в формате "mm.ss". */
  val RECORDING_LEN_PARSER: Parser[Period] = {
    val mmParser = "[0-9]{1,3}".r ^^ str2IntF
    val sep: Parser[String] = "."
    val ssParser = SIXTY_PARSER
    mmParser ~ (sep ~> ssParser) ^^ {
      case mm~ss => new Period().withMinutes(mm).withSeconds(ss)
    }
  }


  // Гостиницы. Тут много терминов, вариантов обозначений и их комбинаций.

  /** Парсер типа питания в гостинице. Обозначения варьируются между конторами, но в целом они похожи. */
  val HOTEL_MEAL_PARSER: Parser[HotelMealType] = {
    import HotelMealTypes._
    // Вспомогательные словечки
    val rest: Parser[String] = "(?i)rest".r
    val breakfast: Parser[String] = "(?i)br[ea]+[kc]+f[ae]st".r
    val bed:  Parser[String] = "(?i)[db][ae][db]".r
    val only: Parser[String] = "(?i)onl[yi]".r
    val half: Parser[String] = "(?i)h[ae]lf".r
    val board: Parser[String] = "(?i)b[oa]{1,2}rd".r
    val sep: Parser[_] = "([&,\\s-]+|and)".r
    val extended = "(?i)ex(t(en[dt][ei]+[dt])?)?\\.?".r | "+"
    val full: Parser[String] = "(?i)f+ul+".r
    val mini: Parser[String] = "(?i)mini".r
    val all: Parser[String] = "(?i)(a(i|l+)|everything)".r | "(?iu)вс[её]".r
    val inclusive = "(?i)in(c(l(u(de[dt]|sive?)?)?)?)?\\.*".r | "(?iu)вкл(ючено)?\\.*".r
    val high: Parser[String] = "(?i)hi([hg][ghn]|-|\\s)?".r
    val clazz: Parser[String] = "(?i)clas+".r
    val ultra: Parser[String] = "(?i)ultr[ao]".r
    // a-la carte, menu
    val ala = "(?i)a[-_]?la".r
    val carte = "(?i)cart[eya]*".r
    val menu = "(?i)men[uy]e?".r | "(?iu)меню"
    val hb: Parser[String] = "HB"
    val fb: Parser[String] = "FB"
    // Готовые наборы буков.
    val onlyBed = ("OB" | "NA" | "RO" | (only ~> bed) | (rest ~> only)) ^^^ OB
    val mealMenu = (ala ~ carte | menu) ^^^ Menu
    val halfBoard = (hb | (half ~ board)) ^^^ HB
    val bedBreakfast = ("BB" | (bed <~ sep ~> breakfast)) ^^^ BB
    // HB+ означает, что это HB, но можно бухать весь день, но закуска платная.
    val extHB = ("HB+" | extended <~ half <~ board | half ~> board ~> extended | extended ~ hb | hb ~ extended) ^^^ ExHB
    // Вся жратва по расписанию, в т.ч. обед и полдник
    val fullBoard = (fb | full ~ board) ^^^ FB
    val extFB = ("FB+" | full ~ board ~ extended | extended ~ full ~ board | extended ~ fb | fb ~ extended) ^^^ ExFB
    // AI/ALL - всё включено, mini - всё с ограничениями
    val ai: Parser[String] = "AI"
    val miniAI = (mini ~ all <~ inclusive | all <~ inclusive ~> mini | mini <~ all | all ~> mini) ^^^ MiniAI
    val normAI = ((all <~ opt(sep) <~ inclusive) | all) ^^^ AI
    // жратва вся нахаляву в любое время дня и ночи, и вообще многое бесплатно
    // TODO Нужно распихать всякие местечковые vip/imperial обозначения между UAI и HCAL
    val hcAI = ("HCA[LI]+".r | (high ~> clazz ~> all ~> inclusive) | (all ~> inclusive ~> high ~> clazz)) ^^^ HCAL
    // Ultra All inclusive. Это для самых упоротых, либо это гостиница-город.
    val ualRe: Parser[String] = "UA[IL]".r
    val uAI = (ualRe | ultra <~ all <~ opt(sep) <~ opt(inclusive) | all ~> opt(sep) ~> opt(inclusive) ~> ultra | ultra <~ ai | ai ~> ultra) ^^^ UAI
    // Собираем конечный парсер на основе готовых сборок
    onlyBed | mealMenu | extHB | halfBoard | miniAI | uAI | hcAI | normAI | extFB | fullBoard | bedBreakfast
  }


  /** Парсер базового описания номера.
   * [[http://www.uatourist.com/docs/info.htm Дока с термами и примерами описаний хат]]. */
  val HOTEL_ROOM_PARSER: Parser[HotelRoomInfo] = {
    import HotelRoomTypes._
    val singleRoom = ("(?i)si?n?gle?".r | "SNG") ^^^ SGL
    val doubleRoom = "(?i)do?u?ble?".r ^^^ DBL
    val twinRoom = "(?i)twi?n".r ^^^ TWN
    val tripleRoom = "(?i)tri?ple?".r ^^^ TRPL
    val quadroRoom = "(?i)qu?a?dr?i?ple?".r ^^^ QDPL
    val exBedSym = 'exbed
    val exBed = "(?i)ex(t(ra)?)?B(ed)?".r ^^^ exBedSym
    val childSym = 'child
    val child = "(?i)chi?ld(ren)?".r ^^^ childSym
    // Счетчик опций: кол-ва детей или доп.кроватей.
    val optCnt: Parser[Int] = opt("\\d+".r) ^^ { _.map(str2IntF) getOrElse 1 }
    val sep: Parser[String] = "([-+\\s]+|with|and)".r
    // Собираем финальный парсер наконец
    val roomBaseParser = singleRoom | doubleRoom | tripleRoom | twinRoom | quadroRoom
    roomBaseParser ~ rep(sep ~> optCnt ~ (exBed | child)) ^^ {
      case hrt ~ opts =>
        val (childrenCnt, exBedCnt) = opts.foldLeft [(Int,Int)] (0 -> 0) {
          case ((childCount, exBedCount), cnt ~ opt) => 
            opt match {
              case s if s == exBedSym  =>  childCount -> (exBedCount + cnt)
              case s if s == childSym  =>  (childCount + cnt) -> exBedCount
            }
        }
        HotelRoomInfo(hrt, childrenCnt=childrenCnt, exBedCnt=exBedCnt)
    }
  }


  /** Кол-во звёзд в гостинице или иная общая характеристика её комфортабельности распознаётся тут. */
  val HOTEL_STARS_PARSER: Parser[HotelStarsLevel] = {
    import HotelStarsLevels._
    // Собираем звёзды
    val star: Parser[String] = "(?i)([*★☆]|[\\s-]*stars?)".r
    val starCount: Parser[Int] = "[1-5]".r ^^ str2IntF
    val hotelStarsCnt = ((star ~> starCount) | (starCount <~ star)) ^^ { forStarCount }
    val hotelStarsLen = rep1(star) ^^ { stars => forStarCount(Math.min(5, stars.size)) }
    val hotelStars = hotelStarsCnt | hotelStarsLen
    // HV-отели
    val hv: Parser[String] = "(?i)hv".r
    val hvSep: Parser[String] = "[-+_/]".r
    val hvLvl: Parser[Int] = "[12]".r ^^ str2IntF
    val hvHotel = (hv ~> opt(hvSep) ~> hvLvl) ^^ { forHvLevel }
    // Финальный парсер звездатости
    hotelStars | hvHotel
  }


  /** Внутренние единицы периода. Используются в промежуточных результатах парсинга из-за отсутствия fold-парсинга. */
  object PeriodUnits extends Enumeration {
    type PeriodUnit = Value
    val Year, Month, Week, Day, hour, minute, second = Value
  }

}


