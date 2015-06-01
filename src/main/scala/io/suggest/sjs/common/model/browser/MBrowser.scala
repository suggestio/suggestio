package io.suggest.sjs.common.model.browser

import io.suggest.sjs.common.model.browser.mozilla.FirefoxDetector
import io.suggest.sjs.common.model.browser.msie.desktop.MsieDesktopDetector
import io.suggest.sjs.common.model.browser.opera.desktop.legacy.OperaLegacyDetector
import io.suggest.sjs.common.model.browser.opera.desktop.next.OperaNextDetector
import io.suggest.sjs.common.model.browser.unknown.UnknownBrowser
import io.suggest.sjs.common.model.browser.webkit.chrome.ChromeDetector
import io.suggest.sjs.common.model.browser.webkit.safari.SafariDetector
import io.suggest.sjs.common.view.safe.nav.SafeNavigator

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:08
 * Description: Модель с детекторами для текущего браузера.
 */
object MBrowser {

  /** Задетекченный браузер. */
  val BROWSER: IBrowser = {
    val uaOpt = SafeNavigator().userAgent
    new MBrowserDetector()
      .detectBrowser(uaOpt)
      .getOrElse { new UnknownBrowser }
  }

}

/** Реализация детектора браузера. Аддоны в обратном порядке применения. */
class MBrowserDetector
  extends OperaLegacyDetector
  with ChromeDetector
  with SafariDetector
  with FirefoxDetector
  with OperaNextDetector
  with MsieDesktopDetector
