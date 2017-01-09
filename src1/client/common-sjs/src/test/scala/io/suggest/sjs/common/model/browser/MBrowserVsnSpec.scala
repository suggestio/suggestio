package io.suggest.sjs.common.model.browser

import minitest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 14:29
 * Description: Тесты для [[MBrowserVsn]].
 */

object MBrowserVsnSpec extends SimpleTestSuite {

  /** parse Vsn Test. */
  private def vt(source: String, expected: Option[MBrowserVsn]): Unit = {
    assertEquals(
      MBrowserVsn.parseMajorMinorVsn(source),
      expected
    )
  }

  test("parse simple version numbers") {
    vt("1.0",   Some(MBrowserVsn(1,   0)) )
    vt("0.34",  Some(MBrowserVsn(0, 34)) )
  }

  test("parse invalid version strings") {
    vt("asdasdasd asdasd", None)
    vt("     ...... asdasdasd", None)
    vt("", None)
    vt(".", None)
  }

  test("Parse opera presto vsn") {
    vt("12.16", Some(MBrowserVsn(12, 16)) )
    vt("12.00", Some(MBrowserVsn(12,  0)) )
    vt("11.50", Some(MBrowserVsn(11, 50)) )
  }

  test("Parse wk-browsers versions") {
    vt("29.0.1795.60",  Some(MBrowserVsn(29, 0)) )    // Opera-next
    vt("42.0.2311.152", Some(MBrowserVsn(42, 0)) )    // Chrome/Chromium
    vt("537.36",        Some(MBrowserVsn(537, 36)) )  // Safari
  }



  private def ppvt(ua: String, prefix: String, result: Option[MBrowserVsn]): Unit = {
    assertEquals(
      MBrowserVsn.parseVsnPrefixedFromUa(ua, prefix),
      result
    )
  }

  test("parse opera vsn from opera-next UA") {
    val onextUa = """Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 OPR/29.0.1795.60"""
    ppvt(onextUa, "OPR/",         Some(MBrowserVsn(29, 0)) )
    ppvt(onextUa, "Chrome/",      Some(MBrowserVsn(42, 0)) )
    ppvt(onextUa, "AppleWebKit/", Some(MBrowserVsn(537, 36)) )
  }

  test("parse firefox vsns from firefox UAs") {
    val ffUa = """Mozilla/5.0 (X11; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0"""
    ppvt(ffUa,  "Firefox/", Some(MBrowserVsn(38, 0)))
    ppvt(ffUa,  "rv:",      Some(MBrowserVsn(38, 0)))
    //ppvt(ffUa,  "Mozilla/", Some(MBrowserVsn(5, 0)))   // TODO Надо бы пофиксить это
    ppvt(ffUa,  "Chrome/",  None)
  }


  test("parse MSIE 9 versions from IE UAs") {
    val ie9Ua = """Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0"""
    ppvt(ie9Ua, "MSIE ",    Some(MBrowserVsn(9, 0)) )
    ppvt(ie9Ua, "Trident/", Some(MBrowserVsn(5, 0)) )
  }

  test("parse MSIE 10 versions") {
    val ie10ua = """Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; Trident/4.0; InfoPath.2; SV1; .NET CLR 2.0.50727; WOW64)"""
    ppvt(ie10ua, "MSIE ",     Some(MBrowserVsn(10, 0)) )
    ppvt(ie10ua, "Trident/",  Some(MBrowserVsn(4, 0)) )
  }

}
