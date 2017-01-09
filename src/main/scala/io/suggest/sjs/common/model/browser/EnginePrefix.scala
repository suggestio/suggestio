package io.suggest.sjs.common.model.browser

import io.suggest.sjs.common.model.browser.mozilla.FirefoxBrowser
import io.suggest.sjs.common.model.browser.msie.desktop.MsieDesktopPrefixing
import io.suggest.sjs.common.model.browser.opera.desktop.OperaPrefixing
import io.suggest.sjs.common.model.browser.webkit.WebKitPrefixing

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.08.15 14:09
 * Description: Поддержка генерации префиксов для браузерных движков.
 */

object EnginePrefix {

  /** Все реализации [[EnginePrefix]] для всех движков. */
  def PREFIXERS = {
    List[EnginePrefix](
      WebKitPrefixing,
      FirefoxBrowser,
      MsieDesktopPrefixing,
      OperaPrefixing
    )
  }

  /** Все известные префиксы. */
  def ALL_PREFIXES = {
    PREFIXERS.iterator
      .map { _.PREFIX }
  }

}

trait EnginePrefix {

  def PREFIX: String

  def CSS_PREFIXING = List("-" + PREFIX + "-")

  def EVENT_PREFIXING = List(PREFIX)

}
