package io.suggest.sjs.common.vm.style

import io.suggest.common.css.CssSzImplicits
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.08.15 16:13
 * Description: API для выставления значения style.top.
 */
trait StyleTop extends IVm with CssSzImplicits {

  override type T <: HTMLElement

  protected def setTop(top: String): this.type = {
    _underlying.style.top = top
    this
  }

  protected def setTopPx(topPx: Int): this.type = {
    setTop(topPx.px)
  }

}
