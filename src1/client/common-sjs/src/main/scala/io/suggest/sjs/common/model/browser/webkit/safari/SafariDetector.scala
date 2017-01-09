package io.suggest.sjs.common.model.browser.webkit.safari

import io.suggest.sjs.common.model.browser.{IBrowser, IBrowserDetector, MBrowserVsn}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 15:50
 * Description: Детектор safari и её версии.
 */
trait SafariDetector extends IBrowserDetector {
  
  override def detectBrowser(uaOpt: Option[String]): Option[IBrowser] = {
    uaOpt filter { ua =>
      ua.contains(" Safari/")
    } flatMap { ua =>
      MBrowserVsn.parseVsnPrefixedFromUa(ua, " Version/") map { vsn =>
        val isMobile = ua.contains("Mobile/")
        SafariBrowser(vsn, isMobile)
      }
    } orElse {
      super.detectBrowser(uaOpt)
    }
  }
  
}
