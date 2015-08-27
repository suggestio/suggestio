package io.suggest.sjs.common.model.browser.webkit.safari

import io.suggest.sjs.common.model.browser.webkit.WebKitPrefixing
import io.suggest.sjs.common.model.browser.{IBrowser, IVendorPrefixer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 11:49
 * Description: Реализация движка css-префиксов для Safari.
 */
trait SafariCssPrefixing extends IVendorPrefixer with IBrowser {

  override def Prefixing = this

  def isMobile: Boolean

  // Шел 2015 год. Чистовой поддержки css-трансформаций в сафари так и нет.

  override def transforms2d: List[String] = {
    val vj = vsnMajor
    if (vj > 3 || (vj == 3 && ((isMobile && vsnMinor >= 2) || (!isMobile && vsnMinor >= 1))) ) {
      WebKitPrefixing.CSS_PREFIXING
    } else {
      NO_SUPPORT
    }
  }

  override def transforms3d: List[String] = {
    val v = vsnMajor
    if ( v >= 4 || (isMobile && (v > 3 || (v == 3 && vsnMinor >= 2))) ) {
      WebKitPrefixing.CSS_PREFIXING
    } else {
      NO_SUPPORT
    }
  }

  /** Поддержка события document visibilitychange характеризуется этими префиксами. */
  override def visibilityChange: List[String] = {
    val _isMobile = isMobile
    val vj = vsnMajor
    if ((_isMobile && vj >= 7) || (!_isMobile && vj >= 6 && vsnMinor >= 1)) {
      super.visibilityChange
    } else {
      NO_SUPPORT
    }
  }
}
