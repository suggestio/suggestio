package io.suggest.sjs.common.model.browser.mozilla

import io.suggest.sjs.common.model.browser.{IBrowserVsn, IBrowser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 11:07
 * Description: Файрфокс.
 */
object FirefoxBrowser {

  /** Список префиксов для mozilla gecko. */
  lazy val MOZ_PREFIXING = List("-moz-")

  def apply(vsn: IBrowserVsn): FirefoxBrowser = {
    FirefoxBrowser(vsn.vsnMajor, vsnMinor = vsn.vsnMinor)
  }

}


case class FirefoxBrowser(
  override val vsnMajor: Int,
  override val vsnMinor: Int = 0    // После ff-4.0 эта версия почти всегда == 0 и не важна.
) extends IBrowser with GeckoCssPrefixing {

  /** Идентификационное имя браузера. */
  override def name = "firefox"
  override def toString = super.toString
}
