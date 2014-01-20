package io.suggest.ym

import org.scalatest._
import YmParsers._
import org.joda.time._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.01.14 22:46
 * Description: Тесты для YmParsers.
 */
class YmParsersTest extends FlatSpec with Matchers {

  private def getF[T](p: Parser[T]): String => T = {s: String => parseAll(p, s).get }

  "DIMENSIONS_PARSER" should "parse different dimensions" in {
    val f = getF(DIMENSIONS_PARSER)
    f("207/23/54")          should be (Dimensions(207.0F, 23.0F, 54.0F))
    f("2000/100/200")       should be (Dimensions(2000, 100, 200))
    f("20000/1500/600")     should be (Dimensions(20000, 1500, 600))
    f("\t\t200/150/150 ")   should be (Dimensions(200, 150, 150))
    f(" 200 / 150 / 150 ")  should be (Dimensions(200, 150, 150))
  }


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
    f("1H").head    should be (1 -> hour)
    f("1H20M50S")   should be (List(1 -> hour, 20 -> minute, 50 -> second))
    f("5000H").head should be (5000 -> hour)
    f("")           should be (Nil)
  }

  "ISO_TIMEPERIOD_PARSER" should "parse ISO 8601 periods" in {
    val f = getF(ISO_PERIOD_PARSER)
    f("P1Y")                 should be (new Period().withYears(1))
    f("P1Y2M10DT")           should be (new Period().withYears(1).withMonths(2).withDays(10))
    f("P1Y2M10D")            should be (new Period().withYears(1).withMonths(2).withDays(10))
    f("PT2H30M")             should be (new Period().withHours(2).withMinutes(30))
    f("P1Y2M10DT2H30M")      should be (new Period().withYears(1).withMonths(2).withDays(10).withHours(2).withMinutes(30))
    f("P")                   should equal (new Period())
  }


  "PLAIN_BOOL_PARSER" should "parse plain-text booleans" in {
    val f = getF(PLAIN_BOOL_PARSER)
    assert(f("true"))
    assert(!f("fAlSe"))
  }

  "WARRANTY_PARSER" should "parse warranty value in different formats" in {
    val f = getF(WARRANTY_PARSER)
    f("true")  should be (Warranty(hasWarranty = true, warrantyPeriod = None))
    f("FALSE") should be (Warranty(hasWarranty = false, warrantyPeriod = None))
    f("P2Y")   should be (Warranty(hasWarranty = true, warrantyPeriod = Some(new Period().withYears(2))))
  }

  "NUMERIC_DATE_PARSER" should "parse dates in different formats" in {
    val f = getF(NUMERIC_DATE_PARSER)
    f("2014/10/10") should be (new LocalDate(2014, 10, 10))
    f("20141010")   should be (new LocalDate(2014, 10, 10))
    f("2014.10.10") should be (new LocalDate(2014, 10, 10))
    f("10.10.2014") should be (new LocalDate(2014, 10, 10))
  }

  "TIME_PARSER" should "parse time in different HIS formats" in {
    val f = getF(TIME_PARSER)
    f("12:23")          should be (new LocalTime(12, 23).withMillisOfSecond(0))
    f("12:03:14")       should be (new LocalTime(12, 3, 14, 0))
    f("12:23+04:00")    should be (new LocalTime(DateTimeZone.forOffsetHours(4)).withHourOfDay(12).withMinuteOfHour(23).withMillisOfSecond(0).withSecondOfMinute(0))
    f("12:23-04:00")    should be (new LocalTime(DateTimeZone.forOffsetHours(-4)).withHourOfDay(12).withMinuteOfHour(23).withMillisOfSecond(0).withSecondOfMinute(0))
    f("12:23:01+03:30") should be (new LocalTime(DateTimeZone.forOffsetHoursMinutes(3, 30)).withHourOfDay(12).withMinuteOfHour(23).withSecondOfMinute(1).withMillisOfSecond(0))
  }

  "DT_PARSER" should "parse datetime in different formats" in {
    val f = getF(DT_PARSER)
    f("2013-02-25 12:03:14") should be (new DateTime(2013, 2, 25, 12, 3, 14, 0))
    f("2013-02-25T12:03:14") should be (new DateTime(2013, 2, 25, 12, 3, 14, 0))
    f("20130225T120314")     should be (new DateTime(2013, 2, 25, 12, 3, 14, 0))
    // TODO Сравниваем через toString из-за того, что почему-то DateTime не совпадают 1:1 при указании любой timezone. Возможно, где-то есть скрытая ошибка.
    f("2013-02-25 12:03:14+04:00").toString() should be (new DateTime(2013, 2, 25, 12, 3, 14, 0, DateTimeZone.forOffsetHours(4)).toString())
  }

  "RECORDING_LEN_PARSER" should "parse mm.ss duration" in {
    val f = getF(RECORDING_LEN_PARSER)
    f("60.00") should be (new Period().withMinutes(60))
    f("90.15") should be (new Period().withMinutes(90).withSeconds(15))
  }


  "MEAL_TYPE_PARSER" should "parse hotel meal types in different formats" in {
    val f = getF(HOTEL_MEAL_PARSER)
    import HotelMealTypes._
    f("HB+") should equal (`HB+`)
    f("all inclusive ultra") should equal (UAI)
    f("all inclusive") should equal (AI)
    f("UAL") should equal (UAI)
    f("HCAI") should equal (HCAL)
    f("ALL INCLUDED ULTRO") should equal (UAI)
    f("RO") should equal (OB)
    f("OB") should equal (OB)
    f("FB+") should equal (`FB+`)
    f("ALL included HI- CLASs") should equal (HCAL)
    f("ExtFB") should equal (`FB+`)
    f("ExFB")  should equal (`FB+`)
    f("FB extended") should equal (`FB+`)
    f("FB") should equal (FB)
    f("full board") should equal (FB)
    f("bad & breakfast") should equal (BB)
    f("miniAI")   should equal (MiniAI)
    f("miniALL")  should equal (MiniAI)
    f("ALL mini") should equal (MiniAI)
    f("mini ALL included") should equal (MiniAI)
    f("всё включено") should equal (AI)
    f("ALL INCLUDED") should equal (AI)
    f("ALL IN") should equal (AI)
    f("ALL-IN") should equal (AI)
    f("ALL-IN ULTRA") should equal (UAI)
    f("All") should equal (AI)
    f("AI")  should equal (AI)
  }


  "HOTEL_ROOM_PARSER" should "parse hotel room specs of different configurations" in {
    val f = getF(HOTEL_ROOM_PARSER)
    import HotelRoomTypes._
    f("SGL") should equal (HotelRoomInfo(SGL))
    f("Dbl+2CHild")  should equal (HotelRoomInfo(DBL, childrenCnt = 2))
    f("DBL + 2Chld") should equal (HotelRoomInfo(DBL, childrenCnt = 2))
    f("Double + 2 children") should equal (HotelRoomInfo(DBL, childrenCnt = 2))
    f("Triple + ExBed") should equal (HotelRoomInfo(TRPL, exBedCnt = 1))
    f("QDPL+Chld") should equal (HotelRoomInfo(QDPL, childrenCnt = 1))
  }


  "HOTEL_STARS_PARSER" should "parse hotel starring" in {
    val f = getF(HOTEL_STARS_PARSER)
    import HotelStarsLevels._
    f("***")  should equal (S3)
    f("hv-1") should equal (HV1)
    f("HV-2") should equal (HV2)
    f("*1")   should equal (S1)
    f("1*")   should equal (S1)
    f("4 *")  should equal (S4)
  }

}
