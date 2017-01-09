package io.suggest.sjs.common.vm.style

import io.suggest.common.css.CssSzImplicits
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:09
 * Description: Аддон для быстрого выставление высоты элементу.
 */
trait StyleHeight extends IVm with CssSzImplicits {

  override type T <: HTMLElement

  protected def setHeight(heightCss: String): Unit = {
    _underlying.style.height = heightCss
  }

  protected def setHeightPx(heightPx: Int): Unit = {
    setHeight(heightPx.px)
  }

}
