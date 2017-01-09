package io.suggest.sjs.common.model.browser.webkit.chrome

import io.suggest.sjs.common.model.browser.webkit.WebKitPrefixing
import io.suggest.sjs.common.model.browser.{IBrowser, IVendorPrefixer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 11:40
 * Description: Реализация движка подстановки css-префиксов для хрома.
 */
trait ChromeCssPrefixing extends IVendorPrefixer with IBrowser {

  override def Prefixing: IVendorPrefixer = this


  override def transforms2d: List[String] = {
    if (vsnMajor >= 36) {
      super.transforms2d

    } else {
      // По всей видимости, prefixed-поддержка была с самых древних версий хрома.
      WebKitPrefixing.CSS_PREFIXING
    }
  }


  override def transforms3d: List[String] = {
    val vj = vsnMajor
    if (vj >= 36) {
      super.transforms3d

    } else if (vj >= 12) {
      WebKitPrefixing.CSS_PREFIXING

    } else {
      NO_SUPPORT
    }
  }

  override def visibilityChange: List[String] = {
    val vj = vsnMajor
    if (vj >= 33) {
      super.visibilityChange

    } else if (vj >= 13) {
      WebKitPrefixing.EVENT_PREFIXING

    } else {
      NO_SUPPORT
    }
  }
}
