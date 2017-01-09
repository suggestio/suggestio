package io.suggest.sjs.common.model.browser.opera.desktop.next

import io.suggest.sjs.common.model.browser.opera.desktop.OperaBrowser
import io.suggest.sjs.common.model.browser.{MBrowserVsn, IBrowser, IBrowserDetector}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 14:19
 * Description: Детектор версии новой оперы.
 */
trait OperaNextDetector extends IBrowserDetector {

  override def detectBrowser(uaOpt: Option[String]): Option[IBrowser] = {
    uaOpt.flatMap { ua =>
      MBrowserVsn.parseVsnPrefixedFromUa(ua, "OPR/")
    } map { oprVsn =>
      OperaBrowser(oprVsn)
    } orElse {
      super.detectBrowser(uaOpt)
    }
  }

}
