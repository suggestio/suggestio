package io.suggest.sjs.common.model.browser.webkit.chrome

import io.suggest.sjs.common.model.browser.{IBrowser, IBrowserDetector, MBrowserVsn}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 15:40
 * Description: Детектор хрома по UA:
 *
 * Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.65 Safari/537.36
 */
trait ChromeDetector extends IBrowserDetector {

  override def detectBrowser(uaOpt: Option[String]): Option[IBrowser] = {
    uaOpt flatMap { ua =>
      MBrowserVsn.parseVsnPrefixedFromUa(ua, "Chrome/")
    } map { vsn =>
      ChromeBrowser(vsn)
    } orElse {
      super.detectBrowser(uaOpt)
    }
  }

}
