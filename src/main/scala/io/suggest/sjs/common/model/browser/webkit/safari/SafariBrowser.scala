package io.suggest.sjs.common.model.browser.webkit.safari

import io.suggest.sjs.common.model.browser.{IBrowserVsn, IBrowser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 11:48
 * Description: Десктопная версия safari.
 */
case class SafariBrowser(
  override val vsnMajor: Int,
  override val vsnMinor: Int,
  override val isMobile: Boolean
)
  extends IBrowser with SafariCssPrefixing
{

  override def name: String = "safari"
  override def toString = super.toString

  /**
   * Мобильная сафари не может скроллить внутренний контейнер, когда можно скроллить внешний.
   * Нужно форсировать появление скроллбара у внутреннего контейнера.
   * TODO Проверить, нуждается ли ipad в таком костыле.
   */
  override def needOverwriteInnerScroll = isMobile

}


object SafariBrowser {

  def apply(vsn: IBrowserVsn, isMobile: Boolean): SafariBrowser = {
    SafariBrowser(vsn.vsnMajor, vsnMinor = vsn.vsnMinor, isMobile)
  }

}
