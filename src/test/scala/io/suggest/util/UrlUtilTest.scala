package io.suggest.util

import org.scalatest._
import matchers.ShouldMatchers
import java.net.URL
import UrlUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.13 10:44
 * Description: scalatests for UrlUtils.
 */
class UrlUtilsTest extends FlatSpec with ShouldMatchers {

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

}
