package io.suggest.sjs.common.model.browser.opera.desktop.legacy

import io.suggest.sjs.common.model.browser.opera.desktop.OperaBrowser
import io.suggest.sjs.common.model.browser.{MBrowserVsn, IBrowser, IBrowserDetector}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 13:37
 * Description: Аддон для определения старой оперы. Opera legacy (presto) можно определить по window.opera.
 */
trait OperaLegacyDetector extends IBrowserDetector {

  override def detectBrowser(uaOpt: Option[String]): Option[IBrowser] = {
    WindowOperaStub()
      .opera
      .flatMap { _.version() }
      .toOption
      .flatMap { MBrowserVsn.parseMajorMinorVsn }
      .orElse {
        uaOpt.flatMap { ua =>
          if (ua contains "Presto/") {
            MBrowserVsn.parseVsnPrefixedFromUa(ua, "Version/")
          } else {
            None
          }
        }
      }
      .map { OperaBrowser.apply }
      .orElse { super.detectBrowser(uaOpt) }
  }

}
