package io.suggest.text.util

import java.net.URL

import UrlUtil._
import org.scalatest.Matchers._
import org.scalatest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.13 10:44
 * Description: scalatests for UrlUtils.
 */
class UrlUtilSpec extends FlatSpec {

  "ensureAbsoluteUrl()" should "absolutize relative urls if baseUrl = suggest.io/a/" in {
    val baseUrl1 = new URL("https://suggest.io/a/")

    ensureAbsoluteUrl(baseUrl1, "/rjvg")    shouldEqual Some(new URL("https://suggest.io/rjvg"))
    ensureAbsoluteUrl(baseUrl1, "rjvg")     shouldEqual Some(new URL("https://suggest.io/a/rjvg"))
    ensureAbsoluteUrl(baseUrl1, "?x=rjvg")  shouldEqual Some(new URL("https://suggest.io/a/?x=rjvg"))
  }

  it should "absolutize rel.urls if baseUrl = suggest.io w/o final slash" in {
    val baseUrl = new URL("https://suggest.io")

    ensureAbsoluteUrl(baseUrl, "/rjvg") shouldEqual Some(new URL("https://suggest.io/rjvg"))
    ensureAbsoluteUrl(baseUrl, "rjvg")  shouldEqual Some(new URL("https://suggest.io/rjvg"))
    ensureAbsoluteUrl(baseUrl, "?x=y")  shouldEqual Some(new URL("https://suggest.io/?x=y"))
    //ensureAbsoluteUrl(baseUrl, "ыыы")  shouldEqual (None)
  }


  "decodeUrl()" should "decode valid URLs as-is" in {
    pcDecodeSafe("https://suggest.io/a_b_c/d") shouldEqual "https://suggest.io/a_b_c/d"
    pcDecodeSafe("https://suggest.io/a%20bc/") shouldEqual "https://suggest.io/a bc/"
    pcDecodeSafe("https://suggest.io/%20%20")  shouldEqual  "https://suggest.io/  "
  }

  it should  "not crash on wrong-encoded URLs" in {
    pcDecodeSafe("https://suggest.io/абвг")    shouldEqual "https://suggest.io/абвг"
    pcDecodeSafe("https://suggest.io/%20абвг") shouldEqual "https://suggest.io/ абвг"
  }


  "normalizeHostname()" should "normalize ASCII domains" in {
    normalizeHostname("www.suggest.ru.") shouldEqual "www.suggest.ru"
    normalizeHostname("www.suggest.ru")  shouldEqual "www.suggest.ru"
  }

  it should "normalize IDN domains" in {
    normalizeHostname("президент.рф")               shouldEqual "xn--d1abbgf6aiiy.xn--p1ai"
    normalizeHostname("ПрЕзиДенТ.рФ")               shouldEqual "xn--d1abbgf6aiiy.xn--p1ai"
    normalizeHostname("xn--d1abbgf6aiiy.xn--p1ai")  shouldEqual "xn--d1abbgf6aiiy.xn--p1ai"
  }


  "normalizePath()" should "re-encode URL path parts" in {
    normalizePath("/asd/b") shouldEqual "/asd/b"
    normalizePath("/wiki/Микрокредит") shouldEqual "/wiki/%D0%9C%D0%B8%D0%BA%D1%80%D0%BE%D0%BA%D1%80%D0%B5%D0%B4%D0%B8%D1%82"
    normalizePath("/wiki/%D0%9C/Микрокредит") shouldEqual "/wiki/%D0%9C/%D0%9C%D0%B8%D0%BA%D1%80%D0%BE%D0%BA%D1%80%D0%B5%D0%B4%D0%B8%D1%82"

    val wikiPath = "/wiki/%D0%9C%D0%B8%D0%BA%D1%80%D0%BE%D0%BA%D1%80%D0%B5%D0%B4%D0%B8%D1%82"
    normalizePath(wikiPath) shouldEqual wikiPath
  }


  "normalizeQuery()" should "re-encode URL query part" in {
    normalizeQuery("fasdf=1&sc&asdf&c=ик") shouldEqual "asdf&c=%D0%B8%D0%BA&fasdf=1&sc"
    normalizeQuery("fasdf=1&sc&asdf&c=%D0%9C") shouldEqual "asdf&c=%D0%9C&fasdf=1&sc"
    normalizeQuery("s=132072df690933835eb8b6ad0b77e7b6f14acad7&a=1") shouldEqual "a=1"
    normalizeQuery("s=132072df690933835eb8b6ad0b77e7b6f14acad7") shouldEqual ""
  }


  "normalize()" should "do everything ok" in {
    normalize("https://www.linux.org.ru/news/conference/8421445") shouldEqual "https://www.linux.org.ru/news/conference/8421445"
    normalize("http://wowgil.ru/?cat=24") shouldEqual "http://wowgil.ru/?cat=24"
    normalize("https://kill.me/?s=132072df690933835eb8b6ad0b77e7b6f14acad7#!asd/asd") shouldEqual "https://kill.me/#!asd/asd"
    normalize("http://президент.рф/путен") shouldEqual "http://xn--d1abbgf6aiiy.xn--p1ai/%D0%BF%D1%83%D1%82%D0%B5%D0%BD"
    normalize("http://ya.ru/asd/asd?q=aasd+ads&hg=1&ie=utf-8") shouldEqual "http://ya.ru/asd/asd?hg=1&ie=utf-8&q=aasd+ads"
    normalize("http://www.aklugovoy.ru/personal_information/blog/Intervention.") shouldEqual "http://www.aklugovoy.ru/personal_information/blog/Intervention."
    normalize("aklugovoy.ru/personal_information/blog/Intervention.") shouldEqual "http://aklugovoy.ru/personal_information/blog/Intervention."
    normalize("шарага.рф/гоги/2") shouldEqual "http://xn--80aaal7d3b.xn--p1ai/%D0%B3%D0%BE%D0%B3%D0%B8/2"
    normalize(" шарага.рф/гоги/2 ") shouldEqual "http://xn--80aaal7d3b.xn--p1ai/%D0%B3%D0%BE%D0%B3%D0%B8/2"
  }

  "humanizeUrl()" should "make simple english URLs human-readable (keep unchanged)" in {
    humanizeUrl("http://ya.ru/lol") shouldEqual "http://ya.ru/lol"
    humanizeUrl("https://ya.ru/lol") shouldEqual "https://ya.ru/lol"
  }
  it should "make IDN-hostnames in URLs human-readable" in {
    humanizeUrl("http://xn--80aaal7d3b.xn--p1ai/") shouldEqual "http://шарага.рф/"
    humanizeUrl("http://xn--80aaal7d3b.xn--p1ai/123") shouldEqual "http://шарага.рф/123"
  }
  it should "make %-encoded URL path/qs in URLs human-readeable" in {
    humanizeUrl("https://www.facebook.com/pages/%D0%91%D0%B0%D1%80-%D0%90%D0%BD%D0%BA%D0%B0/1574446066106309") shouldEqual
      "https://www.facebook.com/pages/Бар-Анка/1574446066106309"
  }
  it should "humanize IDN and %-encoded parts in single URL" in {
    humanizeUrl("https://www.xn--80aaal7d3b.xn--p1ai/p/%D0%91%D0%B0%D1%80-%D0%90%D0%BD%D0%BA%D0%B0-11/123?q=%D0%91%D0%B0%D1%80") shouldEqual
      "https://www.шарага.рф/p/Бар-Анка-11/123?q=Бар"
  }


  "humanizeUrlAggressive()" should "make short and humanized URLs from simple english URLs (only strip common proto)" in {
    humanizeUrlAggressive("http://ya.ru/") shouldEqual "ya.ru"
    humanizeUrlAggressive("http://ya.ru/lol") shouldEqual "ya.ru/lol"
    humanizeUrlAggressive("http://www.ya.ru/lol") shouldEqual "ya.ru/lol"
    humanizeUrlAggressive("https://ya.ru/lol") shouldEqual "ya.ru/lol"
    humanizeUrlAggressive("ftp://ya.ru/lol") shouldEqual "FTP://ya.ru/lol"
  }
  it should "humanize IDNinied URLs" in {
    humanizeUrlAggressive("http://xn--80aaal7d3b.xn--p1ai/") shouldEqual "шарага.рф"
    humanizeUrlAggressive("http://xn--80aaal7d3b.xn--p1ai/123") shouldEqual "шарага.рф/123"
  }
  it should "humanize and minimize facebook russian page URL" in {
    humanizeUrlAggressive("https://www.facebook.com/pages/%D0%91%D0%B0%D1%80-%D0%90%D0%BD%D0%BA%D0%B0/1574446066106309") shouldEqual
      "facebook.com/pages/Бар-Анка/1574446066106309"
  }
  it should "humanize IDN and %-encoded parts in single URL" in {
    humanizeUrlAggressive("http://www.xn--80aaal7d3b.xn--p1ai/p/%D0%91%D0%B0%D1%80-%D0%90%D0%BD%D0%BA%D0%B0-11/123?q=%D0%91%D0%B0%D1%80") shouldEqual
      "шарага.рф/p/Бар-Анка-11/123?q=Бар"
  }


  "isHostnameValid" should "filter-out invalid domains" in {
    isHostnameValid("lenta.ru")          shouldEqual true
    isHostnameValid("newsru.com")        shouldEqual true
    isHostnameValid("f-1.ru")            shouldEqual true
    isHostnameValid("ya.ru")             shouldEqual false
    isHostnameValid("www.ya.ru")         shouldEqual false
    isHostnameValid("www.www1.ya.ru")    shouldEqual false
    isHostnameValid("odnoklasniki.ru")   shouldEqual false
    isHostnameValid("signon.ebay.co.uk") shouldEqual false
    isHostnameValid("google.ru")         shouldEqual false
    isHostnameValid("docs.google.com")   shouldEqual false
    isHostnameValid("www.vk.com")        shouldEqual false
  }


  "isUrlValid" should "filter-out invalid URLs" in {
    isPageUrlValid("http://wowgil.ru/?cat=25") shouldEqual true
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation.JpEg") shouldEqual false
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation.html") shouldEqual true
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation")      shouldEqual true
    isPageUrlValid("http://aklugovoy.ru/personal_information/blog/Georgia_must_pay_compensation.")     shouldEqual true
    isPageUrlValid("http://kino.myvi.ru/search/genre/all?key_words=%D0%B0%D0%BB%D0%B8%D1%81%D0%B0+%D0%B2+%D1%81%D1%82%D1%80%D0%B0%D0%BD%D0%B5+%D1%87%D1%83%D0%B4%D0%B5%D1%81+&page=2") shouldEqual false
  }


  "stripHostnameWww" should "do everything ok" in {
    stripHostnameWww("www.www.kino.ru") shouldEqual "kino.ru"
    stripHostnameWww("www.kino.ru")     shouldEqual "kino.ru"
    stripHostnameWww("kino.ru")         shouldEqual "kino.ru"
  }


  "host2dkey" should "convert hostnames into dkeys" in {
    host2dkey("www.kino.ru")            shouldEqual "kino.ru"
    host2dkey("kino.ru")                shouldEqual "kino.ru"
    host2dkey(".kino.ru.")              shouldEqual "kino.ru"
    host2dkey("www.президент.рф.")      shouldEqual "xn--d1abbgf6aiiy.xn--p1ai"
  }

  "url2dkey" should "convert URLs in dkeys" in {
    url2dkey("http://aversimage.ru/asd/1?gefs=e&x=y") shouldEqual "aversimage.ru"
    url2dkey(new URL("http://aversimage.ru/asd/35"))  shouldEqual "aversimage.ru"
  }

  "url2rowKey" should "convert URLs into HBase row keys (String)" in {
    url2rowKey("http://aversimage.ru/asd/1/?asd=q")   shouldEqual "ru.aversimage/asd/1?asd=q"
    url2rowKey("http://aversimage.ru/")               shouldEqual "ru.aversimage"
    url2rowKey("http://aversimage.ru:80/asd")         shouldEqual "ru.aversimage/asd"
    url2rowKey("http://aversimage.ru:808/asd")        shouldEqual "ru.aversimage/asd"
    url2rowKey("https://aversimage.ru:443/asd")       shouldEqual "ru.aversimage/asd"
    url2rowKey("http://aversimage.ru//")              shouldEqual "ru.aversimage"
    url2rowKey("http://aversimage.ru")                shouldEqual "ru.aversimage"
    url2rowKey("http://aversimage.ru/asd/1/?b=1&a=q") shouldEqual "ru.aversimage/asd/1?a=q&b=1"
    url2rowKey("http://aversimage.ru/asd/1/?a=q&b=1") shouldEqual "ru.aversimage/asd/1?a=q&b=1"
    url2rowKey("https://президент.рф/wiki")           shouldEqual "xn--p1ai.xn--d1abbgf6aiiy/wiki"
  }

  "rowKey2dkey" should "convert hbase rowKeys into site dkeys" in {
    rowKey2dkey("ru.aversimage/123/asd/gsdf")         shouldEqual "aversimage.ru"
    rowKey2dkey("io.suggest/")                        shouldEqual "suggest.io"
    rowKey2dkey("io.suggest")                         shouldEqual "suggest.io"
    rowKey2dkey("ru.avto/a/s/d?asf234=asd&234")       shouldEqual "avto.ru"
  }

}
