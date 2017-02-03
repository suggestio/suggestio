package io.suggest.text.parse.dt

import java.time.LocalDate
import java.util.Locale

import io.suggest.text.parse.dt.DateParseUtil._
import org.scalatest.Matchers._
import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 18:06
 * Description: Тесты к парсеру дат. Сначала идут простые числовые, затем локализованные.
 */
class DateParseUtilSpec extends FlatSpec {

  private val locale_ru = new Locale("ru", "RU")

  /*** Numerical-only dates ***/

  "extractDatesWith()" should "extract YMD-datestamps (YYYYMMDD, 20131022)" in {
    val f = extractDatesWith(re_date_num_ymd_fusion, _:String, maybe_int_month_to_int)

    f("жепь 001 20030101 отаке") shouldEqual List( LocalDate.of(2003, 1, 1) )
    f("жепь 001 2004 333")       shouldEqual Nil
    f("xa 20121213 20130405")    shouldEqual List( LocalDate.of(2013,4,5), LocalDate.of(2012,12,13) )
    f("12 20115454 ff")          shouldEqual Nil
    f("20111111")                shouldEqual List( LocalDate.of(2011, 11, 11) )
    f("http://allnews.com/russia/20120601/liberput.html") shouldEqual List( LocalDate.of(2012, 6, 1) )
    f("")                        shouldEqual Nil
  }

  it should "extract YMD-dates (22.02.2004)" in {
    val f = extractDatesWith(re_date_num_dmy, _:String, maybe_int_month_to_int)

    f("23 2453434 23 asdasd 28.01.2002 fhdgdfthtr")           shouldEqual List( LocalDate.of(2002, 1, 28) )
    f("23 2453434 23 asdasd 28-01-2002 fhdgdfthtr")           shouldEqual List( LocalDate.of(2002, 1, 28) )
    f("76-68-60 olol fuckk")                                  shouldEqual Nil
    f("28.01.2002")                                           shouldEqual List( LocalDate.of(2002, 1, 28) )
    f("")                                                     shouldEqual Nil
    f("http://top.rbc.ru/economics/22/06/2012/656332.shtml")  shouldEqual List( LocalDate.of(2012, 6, 22) )
  }

  it should "extract YMD-dates (2004/02/22, 2004-2-22)" in {
    val f = extractDatesWith(re_date_num_ymd, _:String, maybe_int_month_to_int)

    f("абвгд 23 asdasd 2004/02/22 fhdgdfthtr")      shouldEqual List( LocalDate.of(2004, 2, 22) )
    f("абвгд 23 asdasd 2004.02.22 fhdgdfthtr")      shouldEqual List( LocalDate.of(2004, 2, 22) )
    f("2004.02.22")                                 shouldEqual List( LocalDate.of(2004, 2, 22) )
    f("")                                           shouldEqual Nil
  }

  it should "extract DMY-dates (12 december, 2004)" in {
    val f = extractDatesWith(re_date_loc_dmy, _:String, maybe_loc_month_to_int)

    f("asdfg sdfg dfgreg 13 erfg ef 12 december, 2004 lol!")   shouldEqual List( LocalDate.of(2004,12,12) )
    f("жили-были дед да бабка 9 АВГУСТА 1987 тра-тататата")    shouldEqual List( LocalDate.of(1987, 8, 9) )
    f("adfadsf asd 12/AUG/2004")                               shouldEqual List( LocalDate.of(2004, 8, 12) )
  }

  it should "extract YMD-dates (2004, December 12)" in {
    val f = extractDatesWith(re_date_loc_ymd, _:String, maybe_loc_month_to_int)

    f("Asdfg sdfg dfgreg 13 erfg ef 2004,\n December 12 lol!") shouldEqual List( LocalDate.of(2004, 12, 12) )
    f("Ывап впр рварвпр 2004, декабрь 12 sdfg dgh dfhdf h")    shouldEqual List( LocalDate.of(2004, 12, 12) )
    f("asd asd asd 2004/AUG/12 asd")                           shouldEqual List( LocalDate.of(2004,  8, 12) )
  }

  it should "extract YDM-dates (2004, 12 dec)" in {
    val f = extractDatesWith(re_date_loc_ydm, _:String, maybe_loc_month_to_int)

    f("asd dfhgdsh gh dfg 2004,\n\r 12 dec sgsdf dghf")               shouldEqual List( LocalDate.of(2004, 12, 12) )
    f("фвапр ывапр ыврукые 4 ывап ыап 2004, 12 янв. ывап ывапывап")   shouldEqual List( LocalDate.of(2004,  1, 12) )
  }


  "monthNamesLocalized()" should "generate valid month names for ENGLISH" in {
    val f = monthNamesLocalized(_:Short, Locale.ENGLISH)
    f(1) shouldEqual List("january", "jan")
    f(2) shouldEqual List("february", "feb")
    f(3) shouldEqual List("march", "mar")
  }

  it should "generate valid month names for RUSSIAN" in {
    val f = monthNamesLocalized(_:Short, locale_ru)
    f(1) shouldEqual List("января", "янв")
    f(2) shouldEqual List("февраля", "фев")
    f(9) shouldEqual List("сентября", "сен")
  }


  "monthNamesTrgmDict()" should "generate valid trgm tokens for ENGLISH" in {
    val map = monthNamesTrgmDict(Locale.ENGLISH)
    map("jan") shouldEqual Set(1)
    map("uar") shouldEqual Set(1,2)
    map("dec") shouldEqual Set(12)
    map(" no") shouldEqual Set(11)
  }

  it should "generate valid trgm tokens for RUSSIAN" in {
    val map = monthNamesTrgmDict(locale_ru)
    map("янв") shouldEqual Set(1)
    map(" ян") shouldEqual Set(1)
    map("бря") shouldEqual Set(9,10,11,12)
  }


  "datesTrgmMap" should "contain valid multilang trgm tokens" in {
    val d = datesTrgmMap
    d("янв")   shouldEqual Set(1)
    d("jan")   shouldEqual Set(1)
    d("бря")   shouldEqual Set(9,10,11,12)
  }


  "detectMonth()" should "see proper month in text" in {
    detectMonth("january") shouldEqual Some(1)
    detectMonth("январь")  shouldEqual Some(1)
    detectMonth("ЯнВаРь")  shouldEqual Some(1)
    detectMonth("дикабрь") shouldEqual Some(12)
    detectMonth("Июнь")    shouldEqual Some(6)
    detectMonth("жепь")    shouldEqual None
    detectMonth("июн")     shouldEqual Some(6)
  }


  "extractDates()" should "extract all dates from mass of different strings" in {
    extractDates("asd asd gsdfg in 2002, December 10 adf er hs")        shouldEqual List( LocalDate.of(2002, 12, 10) )
    extractDates("asd asd gsdfg in 2002, December 1 adf er hs")         shouldEqual List( LocalDate.of(2002, 12, 1) )
    extractDates("от 10.12.2002 и вплоть до 12 Декабря 2003 года")      shouldEqual List( LocalDate.of(2003,12,12), LocalDate.of(2002,12,10))
    extractDates("бла-бла-бла 13.13.2013 опа!")                         shouldEqual Nil
    extractDates("бла-бла-бла 11.11.2014 опа!")                         shouldEqual List( LocalDate.of(2014,11,11) )
    extractDates("бла-бла-бла 11.12.2014 опа!")                         shouldEqual List( LocalDate.of(2014,12,11) )
    extractDates("asdfwf 10.01.2014 333")                               shouldEqual List( LocalDate.of(2014, 1,10) )
    extractDates("бла-бла-бла 01.01.2013 опа!")                         shouldEqual List( LocalDate.of(2013, 1, 1) )
    extractDates("бла-бла-бла 2013.01.01 опа!")                         shouldEqual List( LocalDate.of(2013, 1, 1) )
    extractDates("бла-бла-бла 2013\\01\\01 опа!")                       shouldEqual List( LocalDate.of(2013, 1, 1) )
    extractDates("abc-abc-abc 02/AUG/2004 asd")                         shouldEqual List( LocalDate.of(2004, 8, 2) )
    extractDates("abc-abc-abc 2004/AUG/02 asd")                         shouldEqual List( LocalDate.of(2004, 8, 2) )
    extractDates("бла-бла-бла 2013.0.0 sdfasdf!")                       shouldEqual Nil
    extractDates("бла-бла-бла 2013.01.00 daf !")                        shouldEqual Nil
    extractDates("https://site.com/20121212/2345234/23")                shouldEqual List( LocalDate.of(2012, 12, 12) )
    extractDates("asd +792120120111 xc")                                shouldEqual Nil
    extractDates("asd +20120102011 xc")                                 shouldEqual Nil
    extractDates("http://top.rbc.ru/economics/22/06/2012/656332.shtml") shouldEqual List( LocalDate.of(2012, 6, 22) )
    extractDates("http://newsru.com/russia/22jun2012/liberput.html")    shouldEqual List( LocalDate.of(2012, 6, 22) )
    // Проверить високосность. Неправильные даты будут вызывать ошибки на верхних уровнях.
    extractDates("sdf 29-Feb-2012!")                                    shouldEqual List( LocalDate.of(2012, 2, 29) )
    extractDates("sdf 29-Feb-2013!")                                    shouldEqual Nil
  }


  "toDaysCount()" should "convert all dates to days count and back" in {
    dateFromDaysCount(toDaysCount(LocalDate.now))                       shouldEqual LocalDate.now
  }

  it should "convert ancient/future dates without errors" in {
    assert( toDaysCount(LocalDate.of(1555, 11, 11)) >= 0 )
    assert( toDaysCount(LocalDate.of(2555, 11, 11)) >= 0 )
  }

}
