package io.suggest.sjs.common.model.browser

import io.suggest.sjs.common.model.browser.mozilla.FirefoxBrowser
import io.suggest.sjs.common.model.browser.msie.desktop.MsieDesktopBrowser
import io.suggest.sjs.common.model.browser.opera.desktop.OperaBrowser
import io.suggest.sjs.common.model.browser.webkit.chrome.ChromeBrowser
import io.suggest.sjs.common.model.browser.webkit.safari.SafariBrowser
import minitest._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 17:40
 * Description: Тесты для [[MBrowserDetector]].
 */
object MBrowserDetectorSpec extends SimpleTestSuite {

  private def det = new MBrowserDetector

  private def t(ua: String, expected: Option[IBrowser]): Unit = {
    val res = det.detectBrowser( Some(ua) )
    assertEquals(res, expected)
  }

  test("Browser detection should NOT throw any errors") {
    t("Mozilla/5.0", None)
  }

  test("Detect modern Chrome/Chromium via UA") {
    val ua = """Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36"""
    t( ua, Some(ChromeBrowser(43)) )
  }

  test("Detect modern Firefox via UA") {
    val ua = """Mozilla/5.0 (X11; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0"""
    t( ua, Some(FirefoxBrowser(38, 0)) )
  }

  test("Detect legacy opera via UA") {
    val ua = """Opera/9.80 (X11; Linux x86_64) Presto/2.12.388 Version/12.16"""
    t(ua, Some(OperaBrowser(12, 16)) )
  }

  test("Detect chrOpera via UA") {
    val ua = """Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.152 Safari/537.36 OPR/29.0.1795.60"""
    t(ua, Some(OperaBrowser(29, 0)) )
  }

  test("Detect MSIE 10 desktop via UA") {
    val ua = """Mozilla/5.0 (compatible; MSIE 10.6; Windows NT 6.1; Trident/5.0; InfoPath.2; SLCC1; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET CLR 2.0.50727) 3gpp-gba UNTRUSTED/1.0"""
    t(ua, Some(MsieDesktopBrowser(10, 6)) )
  }

  test("Detect desktop Safari") {
    val ua = """Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A"""
    t(ua, Some(SafariBrowser(7, 0, isMobile = false)) )
  }

  test("Detect mobile Safari") {
    val ua = """Mozilla/5.0 (iPad; CPU OS 6_0 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10A5355d Safari/8536.25"""
    t(ua, Some(SafariBrowser(6, 0, isMobile = true)) )
  }

}
