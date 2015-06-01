package io.suggest.sjs.common.model.browser.msie.desktop

import io.suggest.sjs.common.model.browser.{IBrowserVsn, IBrowser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 12:28
 * Description: Модель данных по недобраузеру Internet Exporer.
 */
case class MsieDesktopBrowser(
  override val vsnMajor: Int,
  override val vsnMinor: Int = 0
)
  extends IBrowser with MsieDesktopCssPrefixing
{

  override def name = "msie"
  override def toString = super.toString

}


object MsieDesktopBrowser {

  def apply(vsn: IBrowserVsn): MsieDesktopBrowser = {
    MsieDesktopBrowser(vsn.vsnMajor, vsnMinor = vsn.vsnMinor)
  }

}
