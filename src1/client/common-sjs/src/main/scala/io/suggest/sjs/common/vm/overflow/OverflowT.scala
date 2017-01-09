package io.suggest.sjs.common.vm.overflow

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 17:02
 * Description: Аддон для выставления overflow = hidden.
 */
trait OverflowT extends IVm {

  override type T <: HTMLElement

  def setOverflowHidden(): Unit = {
    _underlying.style.overflow = "hidden"
  }

}
