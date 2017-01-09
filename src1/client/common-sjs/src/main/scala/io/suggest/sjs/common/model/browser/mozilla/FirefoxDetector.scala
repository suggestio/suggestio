package io.suggest.sjs.common.model.browser.mozilla

import io.suggest.sjs.common.model.browser.{MBrowserVsn, IBrowser, IBrowserDetector}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 15:18
 * Description: Детектор файрфокса из UA:
 * Mozilla/5.0 (X11; Linux x86_64; rv:38.0) Gecko/20100101 Firefox/38.0
 */
trait FirefoxDetector extends IBrowserDetector {

  override def detectBrowser(uaOpt: Option[String]): Option[IBrowser] = {
    uaOpt flatMap { ua =>
      MBrowserVsn.parseVsnPrefixedFromUa(ua, "Firefox/")
    } map { vsn =>
      FirefoxBrowser(vsn)
    } orElse {
      super.detectBrowser(uaOpt)
    }
  }

}
