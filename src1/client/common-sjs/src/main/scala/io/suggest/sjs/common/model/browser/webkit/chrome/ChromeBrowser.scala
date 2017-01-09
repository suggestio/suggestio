package io.suggest.sjs.common.model.browser.webkit.chrome

import io.suggest.sjs.common.model.browser.{IBrowser, IBrowserVsn}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 11:35
 * Description: Модель данных по браузерам Chromium/Chrome.
 */

case class ChromeBrowser(
  override val vsnMajor: Int
)
  extends IBrowser with ChromeCssPrefixing
{

  /** Исторически, в хроме минорная версия не важна. */
  override def vsnMinor = 0
  override def name: String = "chrome"
  override def toString = super.toString
}


object ChromeBrowser {
  
  def apply(vsn: IBrowserVsn): ChromeBrowser = {
    ChromeBrowser(vsn.vsnMajor)
  }
  
}
