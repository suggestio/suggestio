package io.suggest.util

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import DateParseUtil._
import java.util.Locale
import com.github.nscala_time.time.Imports._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.03.13 18:06
 * Description: Тесты к парсеру дат. Сначала идут простые числовые, затем локализованные.
 */
class DateParseUtilTest extends FlatSpec with ShouldMatchers {

  val d  = DateTimeFormat.forPattern("dd MMM yyyy").withLocale(Locale.US).parseLocalDate(_:String)
  val dl = { dt:String => List(d(dt)) }
  val empty  = List()
  val locale_ru = new Locale("ru", "RU")

  /*** Numerical-only dates ***/

  "extractDatesWith()" should "extract YMD-datestamps (YYYYMMDD, 20131022)" in {
    val f = extractDatesWith(re_date_num_ymd_fusion, _:String, maybe_int_month_to_int)

    f("жепь 001 20030101 отаке") should equal (dl("01 Jan 2003"))
    f("жепь 001 2004 333")       should equal (empty)
    f("xa 20121213 20130405")    should equal (List(d("13 Dec 2012"), d("05 Apr 2013")).reverse)
    f("12 20115454 ff")          should equal (empty)
    f("20111111")                should equal (dl("11 Nov 2011"))
    f("http://allnews.com/russia/20120601/liberput.html") should equal (dl("01 Jun 2012"))
    f("")                        should equal (empty)
  }

  it should "extract YMD-dates (22.02.2004)" in {
    val f = extractDatesWith(re_date_num_dmy, _:String, maybe_int_month_to_int)

    f("23 2453434 23 asdasd 28.01.2002 fhdgdfthtr") should equal (dl("28 Jan 2002"))
    f("23 2453434 23 asdasd 28-01-2002 fhdgdfthtr") should equal (dl("28 Jan 2002"))
    f("76-68-60 olol fuckk")                        should equal (empty)
    f("28.01.2002")                                 should equal (dl("28 Jan 2002"))
    f("")                                           should equal (empty)
    f("http://top.rbc.ru/economics/22/06/2012/656332.shtml") should equal (dl("22 Jun 2012"))
  }

  it should "extract YMD-dates (2004/02/22, 2004-2-22)" in {
    val f = extractDatesWith(re_date_num_ymd, _:String, maybe_int_month_to_int)

    f("абвгд 23 asdasd 2004/02/22 fhdgdfthtr")      should equal (dl("22 Feb 2004"))
    f("абвгд 23 asdasd 2004.02.22 fhdgdfthtr")      should equal (dl("22 Feb 2004"))
    f("2004.02.22")                                 should equal (dl("22 Feb 2004"))
    f("")                                           should equal (empty)
  }

  it should "extract DMY-dates (12 december, 2004)" in {
    val f = extractDatesWith(re_date_loc_dmy, _:String, maybe_loc_month_to_int)

    f("asdfg sdfg dfgreg 13 erfg ef 12 december, 2004 lol!")   should equal(dl("12 Dec 2004"))
    f("жили-были дед да бабка 9 АВГУСТА 1987 тра-тататата")    should equal(dl("9 Aug 1987"))
    f("adfadsf asd 12/AUG/2004")                               should equal(dl("12 Aug 2004"))
  }

  it should "extract YMD-dates (2004, December 12)" in {
    val f = extractDatesWith(re_date_loc_ymd, _:String, maybe_loc_month_to_int)

    f("Asdfg sdfg dfgreg 13 erfg ef 2004,\n December 12 lol!") should equal(dl("12 Dec 2004"))
    f("Ывап впр рварвпр 2004, декабрь 12 sdfg dgh dfhdf h")    should equal(dl("12 Dec 2004"))
    f("asd asd asd 2004/AUG/12 asd")                           should equal(dl("12 Aug 2004"))
  }

  it should "extract YDM-dates (2004, 12 dec)" in {
    val f = extractDatesWith(re_date_loc_ydm, _:String, maybe_loc_month_to_int)

    f("asd dfhgdsh gh dfg 2004,\n\r 12 dec sgsdf dghf") should equal(dl("12 Dec 2004"))
    f("фвапр ывапр ыврукые 4 ывап ыап 2004, 12 янв. ывап ывапывап") should equal(dl("12 Jan 2004"))
  }


  "monthNamesLocalized()" should "generate valid month names for ENGLISH" in {
    val f = monthNamesLocalized(_:Short, Locale.ENGLISH)
    f(1) should equal (List("january", "jan"))
    f(2) should equal (List("february", "feb"))
    f(3) should equal (List("march", "mar"))
  }

  it should "generate valid month names for RUSSIAN" in {
    val f = monthNamesLocalized(_:Short, locale_ru)
    f(1) should equal (List("январь", "янв"))
    f(2) should equal (List("февраль", "фев"))
    f(9) should equal (List("сентябрь", "сен"))
  }


  "monthNamesTrgmDict()" should "generate valid trgm tokens for ENGLISH" in {
    val map = monthNamesTrgmDict(Locale.ENGLISH)
    map("jan") should equal (Set(1))
    map("uar") should equal (Set(1,2))
    map("dec") should equal (Set(12))
    map(" no") should equal (Set(11))
  }

  it should "generate valid trgm tokens for RUSSIAN" in {
    val map = monthNamesTrgmDict(locale_ru)
    map("янв") should equal (Set(1))
    map(" ян") should equal (Set(1))
    map("брь") should equal (Set(9,10,11,12))
  }


  "datesTrgmMap" should "contain valid multilang trgm tokens" in {
    val d = datesTrgmMap
    d("янв")   should equal (Set(1))
    d("jan")   should equal (Set(1))
    d("брь")   should equal (Set(9,10,11,12))
  }


  "detectMonth()" should "see proper month in text" in {
    detectMonth("january") should equal (Some(1))
    detectMonth("январь")  should equal (Some(1))
    detectMonth("ЯнВаРь")  should equal (Some(1))
    detectMonth("дикабрь") should equal (Some(12))
    detectMonth("Июнь")    should equal (Some(6))
    detectMonth("жепь")    should equal (None)
    detectMonth("июн")     should equal (Some(6))
  }


  "extractDates()" should "extract all dates from mass of different strings" in {
    extractDates("asd asd gsdfg in 2002, December 10 adf er hs")        should equal(dl("10 Dec 2002"))
    extractDates("asd asd gsdfg in 2002, December 1 adf er hs")         should equal(dl("1 Dec 2002"))
    extractDates("от 10.12.2002 и вплоть до 12 Декабря 2003 года")      should equal(List(d("12 Dec 2003"), d("10 Dec 2002")))
    extractDates("бла-бла-бла 13.13.2013 опа!")                         should equal(empty)
    extractDates("бла-бла-бла 01.01.2013 опа!")                         should equal(dl("01 Jan 2013"))
    extractDates("бла-бла-бла 2013.01.01 опа!")                         should equal(dl("01 Jan 2013"))
    extractDates("бла-бла-бла 2013\\01\\01 опа!")                       should equal(dl("01 Jan 2013"))
    extractDates("abc-abc-abc 02/AUG/2004 asd")                         should equal(dl("02 Aug 2004"))
    extractDates("abc-abc-abc 2004/AUG/02 asd")                         should equal(dl("02 Aug 2004"))
    extractDates("бла-бла-бла 2013.0.0 sdfasdf!")                       should equal(empty)
    extractDates("бла-бла-бла 2013.01.00 daf !")                        should equal(empty)
    extractDates("https://site.com/20121212/2345234/23")                should equal(dl("12 Dec 2012"))
    extractDates("asd +792120120111 xc")                                should equal(empty)
    extractDates("asd +20120102011 xc")                                 should equal(empty)
    extractDates("http://top.rbc.ru/economics/22/06/2012/656332.shtml") should equal(dl("22 Jun 2012"))
    extractDates("http://newsru.com/russia/22jun2012/liberput.html")    should equal(dl("22 Jun 2012"))
    // Проверить високосность. Неправильные даты будут вызывать ошибки на верхних уровнях.
    extractDates("sdf 29-Feb-2012!")                                    should equal(dl("29 Feb 2012"))
    extractDates("sdf 29-Feb-2013!")                                    should equal(empty)
  }


  "toDaysCount()" should "convert all dates to days count and back" in {
    dateFromDaysCount(toDaysCount(LocalDate.now))                       should equal (LocalDate.now)
  }

}
