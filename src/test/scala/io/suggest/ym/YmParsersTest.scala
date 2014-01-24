package io.suggest.ym

import org.scalatest._
import YmParsers._
import org.joda.time._
import model._

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
    f("207/23/54")          shouldBe Dimensions(207.0F, 23.0F, 54.0F)
    f("2000/100/200")       shouldBe Dimensions(2000, 100, 200)
    f("20000/1500/600")     shouldBe Dimensions(20000, 1500, 600)
    f("\t\t200/150/150 ")   shouldBe Dimensions(200, 150, 150)
    f(" 200 / 150 / 150 ")  shouldBe Dimensions(200, 150, 150)
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

  "WARRANTY_PARSER" should "parse warranty value in different formats" in {
    val f = getF(WARRANTY_PARSER)
    f("true")  shouldBe WarrantyNoPeriod
    f("FALSE") shouldBe NoWarranty
    f("P2Y")   shouldBe HasWarranty(new Period().withYears(2))
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

  "RECORDING_LEN_PARSER" should "parse mm.ss duration" in {
    val f = getF(RECORDING_LEN_PARSER)
    f("60.00") shouldBe new Period().withMinutes(60)
    f("90.15") shouldBe new Period().withMinutes(90).withSeconds(15)
  }


  "MEAL_TYPE_PARSER" should "parse hotel meal types in different formats" in {
    val f = getF(HOTEL_MEAL_PARSER)
    import HotelMealTypes._
    f("HB+")                  shouldEqual ExHB
    f("all inclusive ultra")  shouldEqual UAI
    f("all inclusive")        shouldEqual AI
    f("UAL")                  shouldEqual UAI
    f("HCAI")                 shouldEqual HCAL
    f("ALL INCLUDED ULTRO")   shouldEqual UAI
    f("RO")                   shouldEqual OB
    f("OB")                   shouldEqual OB
    f("FB+")                  shouldEqual ExFB
    f("ALL included HI- CLASs") shouldEqual HCAL
    f("ExtFB")                shouldEqual ExFB
    f("ExFB")                 shouldEqual ExFB
    f("FB extended")          shouldEqual ExFB
    f("FB")                   shouldEqual FB
    f("full board")           shouldEqual FB
    f("bad & breakfast")      shouldEqual BB
    f("miniAI")               shouldEqual MiniAI
    f("miniALL")              shouldEqual MiniAI
    f("ALL mini")             shouldEqual MiniAI
    f("mini ALL included")    shouldEqual MiniAI
    f("всё включено")         shouldEqual AI
    f("ALL INCLUDED")         shouldEqual AI
    f("ALL IN")               shouldEqual AI
    f("ALL-IN")               shouldEqual AI
    f("ALL-IN ULTRA")         shouldEqual UAI
    f("All")                  shouldEqual AI
    f("AI")                   shouldEqual AI
  }


  "HOTEL_ROOM_PARSER" should "parse hotel room specs of different configurations" in {
    val f = getF(HOTEL_ROOM_PARSER)
    import HotelRoomTypes._
    f("SGL")                  shouldEqual HotelRoomInfo(SGL)
    f("Dbl+2CHild")           shouldEqual HotelRoomInfo(DBL, childrenCnt = 2)
    f("DBL + 2Chld")          shouldEqual HotelRoomInfo(DBL, childrenCnt = 2)
    f("Double + 2 children")  shouldEqual HotelRoomInfo(DBL, childrenCnt = 2)
    f("Triple + ExBed")       shouldEqual HotelRoomInfo(TRPL, exBedCnt = 1)
    f("QDPL+Chld")            shouldEqual HotelRoomInfo(QDPL, childrenCnt = 1)
  }


  "HOTEL_STARS_PARSER" should "parse hotel starring" in {
    val f = getF(HOTEL_STARS_PARSER)
    import HotelStarsLevels._
    f("***")  shouldEqual S3
    f("hv-1") shouldEqual HV1
    f("HV2")  shouldEqual HV2
    f("*1")   shouldEqual S1
    f("1*")   shouldEqual S1
    f("4 *")  shouldEqual S4
  }


  "PARAM_NAME_PARSER" should "parse different param names into known terms" in {
    val f = getF(PARAM_NAME_PARSER)
    import ParamNames._
    f("Color")            shouldEqual Color
    f("COLOUR")           shouldEqual Color
    f("Цвет")             shouldEqual Color
    f("ЦвЕт")             shouldEqual Color
    f("Size")             shouldEqual Size
    f("Размер")           shouldEqual Size
    f("Пол")              shouldEqual Gender
    f("Gender")           shouldEqual Gender
    f("Возраст")          shouldEqual Age
    f("agE")              shouldEqual Age
    f("материал")         shouldEqual Material
    f("Матерьиал")        shouldEqual Material
    f("MaTeriaLs")        shouldEqual Material
    f("КаПюшон")          shouldEqual Hood
    f("HOOD")             shouldEqual Hood
    f("длинНа")           shouldEqual Length
    f("Длина куртки")     shouldEqual JacketLength
    f("Высота каблука")   shouldEqual HeelHeight
    f("heel HeIght")      shouldEqual HeelHeight
    f("heigth")           shouldEqual Height
    f("вЫсОтА")           shouldEqual Height
    f("Чашка")            shouldEqual Cup
    f("Размер чашки")     shouldEqual Cup
    f("Size of cup")      shouldEqual Cup
    f("Cup")              shouldEqual Cup
    f("Cup size")         shouldEqual Cup
    f("Обхват груди")     shouldEqual Chest
    f("Chest")            shouldEqual Chest
    f("Размер трусов")    shouldEqual PantsSize
    f("Трусов размер")    shouldEqual PantsSize
    f("Pants size")       shouldEqual PantsSize
    f("Size of pant")     shouldEqual PantsSize
    f("Ширина")           shouldEqual Width
    f("Widht")            shouldEqual Width
    f("Объем")            shouldEqual Volume
    f("Volume")           shouldEqual Volume
    f("Вес")              shouldEqual Weight
    f("Weigth")           shouldEqual Weight
    f("Масса")            shouldEqual Weight
    f("Mass")             shouldEqual Weight
    f("Рост")             shouldEqual Growth
    f("Growht")           shouldEqual Growth
    f("Waist")            shouldEqual Waist
    f("Waistline")        shouldEqual Waist
    f("Талия")            shouldEqual Waist
    f("Обхват талии")     shouldEqual Waist
    f("Sleeve")           shouldEqual Sleeve
    f("Рукав")            shouldEqual Sleeve
    f("Длина рукава")     shouldEqual Sleeve
    f("Sleeve length")    shouldEqual Sleeve
    f("Cuff")             shouldEqual Cuff
    f("Манжеты")          shouldEqual Cuff
    f("Shoulder")         shouldEqual Shoulder
    f("Ширина плеч")      shouldEqual Shoulder
    f("Плечо")            shouldEqual Shoulder
  }

}
