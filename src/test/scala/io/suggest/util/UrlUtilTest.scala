package io.suggest.util

import org.scalatest._
import java.net.URL
import UrlUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.13 10:44
 * Description: scalatests for UrlUtils.
 */
class UrlUtilTest extends FlatSpec with Matchers {

  "ensureAbsoluteUrl()" should "absolutize relative urls if baseUrl = suggest.io/a/" in {
    val baseUrl1 = new URL("https://suggest.io/a/")

    ensureAbsoluteUrl(baseUrl1, "/rjvg") should equal (Some(new URL("https://suggest.io/rjvg")))
    ensureAbsoluteUrl(baseUrl1, "rjvg") should equal (Some(new URL("https://suggest.io/a/rjvg")))
    ensureAbsoluteUrl(baseUrl1, "?x=rjvg") should equal (Some(new URL("https://suggest.io/a/?x=rjvg")))
  }

  it should "absolutize rel.urls if baseUrl = suggest.io w/o final slash" in {
    val baseUrl = new URL("https://suggest.io")

    ensureAbsoluteUrl(baseUrl, "/rjvg") should equal (Some(new URL("https://suggest.io/rjvg")))
    ensureAbsoluteUrl(baseUrl, "rjvg") should equal (Some(new URL("https://suggest.io/rjvg")))
    ensureAbsoluteUrl(baseUrl, "?x=y") should equal (Some(new URL("https://suggest.io/?x=y")))
    //UrlUtils.ensureAbsoluteUrl(baseUrl, "ыыы")  should equal (None)
  }


  "decodeUrl()" should "decode valid URLs as-is" in {
    decodeUrl("https://suggest.io/a_b_c/d") should equal ("https://suggest.io/a_b_c/d")
    decodeUrl("https://suggest.io/a%20bc/") should equal ("https://suggest.io/a bc/")
    decodeUrl("https://suggest.io/%20%20") should equal  ("https://suggest.io/  ")
  }

  it should  "not crash on wrong-encoded URLs" in {
    decodeUrl("https://suggest.io/абвг") should equal ("https://suggest.io/абвг")
    decodeUrl("https://suggest.io/%20абвг") should equal ("https://suggest.io/ абвг")
  }


  "normalizeHostname()" should "normalize ASCII domains" in {
    normalizeHostname("www.suggest.ru.") should equal ("www.suggest.ru")
    normalizeHostname("www.suggest.ru")  should equal ("www.suggest.ru")
  }

  it should "normalize IDN domains" in {
    normalizeHostname("президент.рф") should equal ("xn--d1abbgf6aiiy.xn--p1ai")
    normalizeHostname("ПрЕзиДенТ.рФ") should equal ("xn--d1abbgf6aiiy.xn--p1ai")
    normalizeHostname("xn--d1abbgf6aiiy.xn--p1ai") should equal ("xn--d1abbgf6aiiy.xn--p1ai")
  }


  "normalizePath()" should "re-encode URL path parts" in {
    normalizePath("/asd/b") should equal ("/asd/b")
    normalizePath("/wiki/Микрокредит") should equal ("/wiki/%D0%9C%D0%B8%D0%BA%D1%80%D0%BE%D0%BA%D1%80%D0%B5%D0%B4%D0%B8%D1%82")
    normalizePath("/wiki/%D0%9C/Микрокредит") should equal ("/wiki/%D0%9C/%D0%9C%D0%B8%D0%BA%D1%80%D0%BE%D0%BA%D1%80%D0%B5%D0%B4%D0%B8%D1%82")

    val wikiPath = "/wiki/%D0%9C%D0%B8%D0%BA%D1%80%D0%BE%D0%BA%D1%80%D0%B5%D0%B4%D0%B8%D1%82"
    normalizePath(wikiPath) should equal (wikiPath)
  }


  "normalizeQuery()" should "re-encode URL query part" in {
    normalizeQuery("fasdf=1&sc&asdf&c=ик") should equal ("asdf&c=%D0%B8%D0%BA&fasdf=1&sc")
    normalizeQuery("fasdf=1&sc&asdf&c=%D0%9C") should equal ("asdf&c=%D0%9C&fasdf=1&sc")
    normalizeQuery("s=132072df690933835eb8b6ad0b77e7b6f14acad7&a=1") should equal ("a=1")
    normalizeQuery("s=132072df690933835eb8b6ad0b77e7b6f14acad7") should equal ("")
  }


  "normalize()" should "do everything ok" in {
    normalize("https://www.linux.org.ru/news/conference/8421445") should equal("https://www.linux.org.ru/news/conference/8421445")
    normalize("http://wowgil.ru/?cat=24") should equal ("http://wowgil.ru/?cat=24")
    normalize("https://kill.me/?s=132072df690933835eb8b6ad0b77e7b6f14acad7#!asd/asd") should equal ("https://kill.me/#!asd/asd")
    normalize("http://президент.рф/путен") should equal ("http://xn--d1abbgf6aiiy.xn--p1ai/%D0%BF%D1%83%D1%82%D0%B5%D0%BD")
    normalize("http://ya.ru/asd/asd?q=aasd+ads&hg=1&ie=utf-8") should equal ("http://ya.ru/asd/asd?hg=1&ie=utf-8&q=aasd+ads")
  }


  "isHostnameValid" should "filter-out invalid domains" in {
    isHostnameValid("lenta.ru")          should equal (true)
    isHostnameValid("newsru.com")        should equal (true)
    isHostnameValid("f-1.ru")            should equal (true)
    isHostnameValid("ya.ru")             should equal (false)
    isHostnameValid("www.ya.ru")         should equal (false)
    isHostnameValid("www.www1.ya.ru")    should equal (false)
    isHostnameValid("odnoklasniki.ru")   should equal (false)
    isHostnameValid("signon.ebay.co.uk") should equal (false)
    isHostnameValid("google.ru")         should equal (false)
    isHostnameValid("docs.google.com")   should equal (false)
    isHostnameValid("www.vk.com")        should equal (false)
  }


  "isUrlValid" should "filter-out invalid URLs" in {
    isPageUrlValid("http://wowgil.ru/?cat=25") should equal (true)
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation.JpEg") should equal (false)
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation.html") should equal (true)
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation")      should equal (true)
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation.")     should equal (true)
    isPageUrlValid("http://kino.myvi.ru/search/genre/all?key_words=%D0%B0%D0%BB%D0%B8%D1%81%D0%B0+%D0%B2+%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B5+%D1%87%D1%83%D0%B4%D0%B5%D1%81+&page=2") should equal (false)
  }


  "stripHostnameWww" should "do everything ok" in {
    stripHostnameWww("www.www.kino.ru") should equal ("kino.ru")
    stripHostnameWww("www.kino.ru")     should equal ("kino.ru")
    stripHostnameWww("kino.ru")         should equal ("kino.ru")
  }


  "host2dkey" should "convert hostnames into dkeys" in {
    host2dkey("www.kino.ru")            should equal ("kino.ru")
    host2dkey("kino.ru")                should equal ("kino.ru")
    host2dkey(".kino.ru.")              should equal ("kino.ru")
    host2dkey("www.президент.рф.")      should equal ("xn--d1abbgf6aiiy.xn--p1ai")
  }

  "url2dkey" should "convert URLs in dkeys" in {
    url2dkey("http://aversimage.ru/asd/1?gefs=e&x=y") should equal ("aversimage.ru")
    url2dkey(new URL("http://aversimage.ru/asd/35"))  should equal ("aversimage.ru")
  }

  "url2rowKey" should "convert URLs into HBase row keys (String)" in {
    url2rowKey("http://aversimage.ru/asd/1/?asd=q")   should equal ("ru.aversimage/asd/1?asd=q")
    url2rowKey("http://aversimage.ru/")               should equal ("ru.aversimage")
    url2rowKey("http://aversimage.ru:80/asd")         should equal ("ru.aversimage/asd")
    url2rowKey("http://aversimage.ru:808/asd")        should equal ("ru.aversimage/asd")
    url2rowKey("https://aversimage.ru:443/asd")       should equal ("ru.aversimage/asd")
    url2rowKey("http://aversimage.ru//")              should equal ("ru.aversimage")
    url2rowKey("http://aversimage.ru")                should equal ("ru.aversimage")
    url2rowKey("http://aversimage.ru/asd/1/?b=1&a=q") should equal ("ru.aversimage/asd/1?a=q&b=1")
    url2rowKey("http://aversimage.ru/asd/1/?a=q&b=1") should equal ("ru.aversimage/asd/1?a=q&b=1")
    url2rowKey("https://президент.рф/wiki")           should equal ("xn--p1ai.xn--d1abbgf6aiiy/wiki")
  }

  "rowKey2dkey" should "convert hbase rowKeys into site dkeys" in {
    rowKey2dkey("ru.aversimage/123/asd/gsdf")         should equal ("aversimage.ru")
    rowKey2dkey("io.suggest/")                        should equal ("suggest.io")
    rowKey2dkey("io.suggest")                         should equal ("suggest.io")
    rowKey2dkey("ru.avto/a/s/d?asf234=asd&234")       should equal ("avto.ru")
  }

}
