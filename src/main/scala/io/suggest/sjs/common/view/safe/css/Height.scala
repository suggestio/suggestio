package io.suggest.sjs.common.view.safe.css

import io.suggest.sjs.common.view.safe.ISafe
import io.suggest.sjs.common.view.vutil.CssSzImplicits
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:09
 * Description: Аддон для быстрого выставление высоты элементу.
 */
trait Height extends ISafe with CssSzImplicits {

  override type T <: HTMLElement

  protected def setHeight(heightCss: String): Unit = {
    _underlying.style.height = heightCss
  }

  protected def setHeightPx(heightPx: Int): Unit = {
    setHeight(heightPx.px)
  }

}
