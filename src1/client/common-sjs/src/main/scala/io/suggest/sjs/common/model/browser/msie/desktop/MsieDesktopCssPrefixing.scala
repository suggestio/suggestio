package io.suggest.sjs.common.model.browser.msie.desktop

import io.suggest.sjs.common.model.browser.{IBrowser, IVendorPrefixer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 12:30
 * Description: Реализация css-префиксинга для MSIE.
 */
trait MsieDesktopCssPrefixing extends IVendorPrefixer with IBrowser {

  override def Prefixing = this

  override def transforms2d: List[String] = {
    val v = vsnMajor
    if (v >= 10) {
      super.transforms2d

    } else if (v >= 9) {
      MsieDesktopPrefixing.CSS_PREFIXING

    } else {
      NO_SUPPORT
    }
  }

  override def transforms3d: List[String] = {
    if (vsnMajor >= 10) {
      super.transforms3d
    } else {
      NO_SUPPORT
    }
  }

  override def visibilityChange: List[String] = {
    if (vsnMajor >= 10) {
      super.visibilityChange
    } else {
      NO_SUPPORT
    }
  }
}
