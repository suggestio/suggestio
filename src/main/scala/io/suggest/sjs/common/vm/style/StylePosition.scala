package io.suggest.sjs.common.vm.style

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:42
 * Description: Аддон для быстрого выставления тега position.
 */
trait StylePosition extends IVm {
  
  override type T <: HTMLElement
  
  protected def setPosition(mode: String): Unit = {
    _underlying.style.position = mode
  }
  
  def positionAbsolute(): Unit = {
    setPosition("absolute")
  }

}
