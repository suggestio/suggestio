package io.suggest.ym

import org.scalatest._
import org.joda.time._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.01.14 22:46
 * Description: Тесты для YmParsers.
 */
class YmParsersTest extends FlatSpec with Matchers {

  private lazy val parsers = new YmParsers

  import parsers._

  private def getF[T](p: Parser[T]): String => T = {s: String => parseAll(p, s).get }


  "ISO_PERIOD_DATE_PARSER" should "parse ISO 8601 period date parts" in {
    val f = getF(ISO_PERIOD_DATE_PARSER).andThen { _.map { s => s._1 -> s._2 } }
    import PeriodUnits._
    f("1Y").head    should be (1   -> Year)
    f("5Y").head    should be (5   -> Year)
    f("20Y").head   should be (20  -> Year)
    f("1M").head    should be (1   -> Month)
    f("20M").head   should be (20  -> Month)
    f("944M").head  should be (944 -> Month)
    f("1Y6M")       should be (List(1 -> Year, 6 -> Month))
    f("3000D").head should be (3000 -> Day)
    f("2Y3M10D")    should be (List(2 -> Year, 3 -> Month, 10 -> Day))
    f("")           should be (Nil)
  }

  "ISO_PERIOD_TIME_PARSER" should "parse ISO 8601 period time parts" in {
    val f = getF(ISO_PERIOD_TIME_PARSER).andThen { _.map { s => s._1 -> s._2 } }
    import PeriodUnits._
    f("1H").head    shouldBe (1 -> hour)
    f("1H20M50S")   shouldBe List(1 -> hour, 20 -> minute, 50 -> second)
    f("5000H").head shouldBe (5000 -> hour)
    f("")           shouldBe Nil
  }

  "ISO_TIMEPERIOD_PARSER" should "parse ISO 8601 periods" in {
    val f = getF(ISO_PERIOD_PARSER)
    f("P1Y")                 shouldBe  new Period().withYears(1)
    f("P1Y2M10DT")           shouldBe  new Period().withYears(1).withMonths(2).withDays(10)
    f("P1Y2M10D")            shouldBe  new Period().withYears(1).withMonths(2).withDays(10)
    f("PT2H30M")             shouldBe  new Period().withHours(2).withMinutes(30)
    f("P1Y2M10DT2H30M")      shouldBe  new Period().withYears(1).withMonths(2).withDays(10).withHours(2).withMinutes(30)
    f("P")                   shouldEqual new Period()
  }


  "PLAIN_BOOL_PARSER" should "parse plain-text booleans" in {
    val f = getF(PLAIN_BOOL_PARSER)
    assert(f("true"))
    assert(!f("fAlSe"))
  }

  "BOOL_PARSER" should "parse different data into booleans" in {
    val f = getF(BOOL_PARSER)
    f("true")       shouldBe true
    f("FALSE")      shouldBe false
    f("tRuE")       shouldBe true
    f("1")          shouldBe true
    f("0")          shouldBe false
    f("111")        shouldBe true
    f("yes")        shouldBe true
    f("No")         shouldBe false
    f("YeS")        shouldBe true
    f("+")          shouldBe true
    f("-")          shouldBe false
    f("нету")       shouldBe false
    f("нет")        shouldBe false
    f("Есть")       shouldBe true
    f("ДА")         shouldBe true
  }

  "NUMERIC_DATE_PARSER" should "parse dates in different formats" in {
    val f = getF(NUMERIC_DATE_PARSER)
    f("2014/10/10") shouldBe new LocalDate(2014, 10, 10)
    f("20141010")   shouldBe new LocalDate(2014, 10, 10)
    f("2014.10.10") shouldBe new LocalDate(2014, 10, 10)
    f("10.10.2014") shouldBe new LocalDate(2014, 10, 10)
  }

  "TIME_PARSER" should "parse time in different HIS formats" in {
    val f = getF(TIME_PARSER)
    f("12:23")          shouldBe new LocalTime(12, 23).withMillisOfSecond(0)
    f("12:03:14")       shouldBe new LocalTime(12, 3, 14, 0)
    f("12:23+04:00")    shouldBe new LocalTime(DateTimeZone.forOffsetHours(4)).withHourOfDay(12).withMinuteOfHour(23).withMillisOfSecond(0).withSecondOfMinute(0)
    f("12:23-04:00")    shouldBe new LocalTime(DateTimeZone.forOffsetHours(-4)).withHourOfDay(12).withMinuteOfHour(23).withMillisOfSecond(0).withSecondOfMinute(0)
    f("12:23:01+03:30") shouldBe new LocalTime(DateTimeZone.forOffsetHoursMinutes(3, 30)).withHourOfDay(12).withMinuteOfHour(23).withSecondOfMinute(1).withMillisOfSecond(0)
  }

  "DT_PARSER" should "parse datetime in different formats" in {
    val f = getF(DT_PARSER)
    f("2013-02-25 12:03:14") shouldBe new DateTime(2013, 2, 25, 12, 3, 14, 0)
    f("2013-02-25T12:03:14") shouldBe new DateTime(2013, 2, 25, 12, 3, 14, 0)
    f("20130225T120314")     shouldBe new DateTime(2013, 2, 25, 12, 3, 14, 0)
    // TODO Сравниваем через toString: Почему-то DateTime не совпадают 1:1 при указании любой timezone. Возможно, где-то есть скрытая ошибка.
    f("2013-02-25 12:03:14+04:00").toString() shouldBe new DateTime(2013, 2, 25, 12, 3, 14, 0, DateTimeZone.forOffsetHours(4)).toString()
  }

}
