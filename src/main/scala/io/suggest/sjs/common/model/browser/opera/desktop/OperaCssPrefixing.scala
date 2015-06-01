package io.suggest.sjs.common.model.browser.opera.desktop

import io.suggest.sjs.common.model.browser.webkit.WebKitPrefixing
import io.suggest.sjs.common.model.browser.{IBrowser, IVendorPrefixer}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 12:08
 * Description: Поддержка css-префиксов для Opera Browser.
 */

// TODO Возможно, эту единую оперу надо распилить на next и legacy.

trait OperaCssPrefixing extends IVendorPrefixer with IBrowser {

  override def CssPrefixing = this

  /** Поддерка css-трансформаций есть в большинстве опер. */
  override def transforms2d: List[String] = {
    val v = vsnMajor
    // TODO Отличить хроперу от хрома тяжело, поэтому все версии выше v12 бесполезны вроде бы.
    if (v >= 23 || (v == 12 && vsnMinor >= 10)) {
      // Opera Blink имеет чистовую поддержку трансформаций.
      super.transforms2d

    } else if (v >= 15) {
      // Opera webkit умеет черновую поддержку с preview-релизов.
      WebKitPrefixing.PREFIXING

    } else if (v > 10 || (v == 10 && vsnMinor >= 5)) {
      // Opera presto умеет prefixed css-transform после 10.5
      OperaPrefixing.OPERA_PREFIXING

    } else {
      // Opera < 10.5 вообще не умеет css-анимацию
      NO_SUPPORT
    }
  }


  /** Поддержка transform3d есть только в opera next (webkit/blink). */
  override def transforms3d: List[String] = {
    val v = vsnMajor
    if (v >= 23) {
      super.transforms3d
    } else if (v >= 15) {
      WebKitPrefixing.PREFIXING
    } else {
      NO_SUPPORT
    }
  }

}
