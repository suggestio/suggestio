package io.suggest.sjs.common.model.browser.msie.desktop

import io.suggest.sjs.common.model.browser.{MBrowserVsn, IBrowser, IBrowserDetector}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:10
 * Description: Аддон детектора для IE.
 */
trait MsieDesktopDetector extends IBrowserDetector {

  override def detectBrowser(uaOpt: Option[String]): Option[IBrowser] = {
    uaOpt flatMap { ua =>
      MBrowserVsn.parseVsnPrefixedFromUa(ua, "MSIE ")
    } map { vsn =>
      MsieDesktopBrowser(vsn)
    } orElse {
      super.detectBrowser(uaOpt)
    }
  }

}
