package io.suggest.sjs.common.vm.content

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 19:06
 * Description: Поддержка реплейса узла DOM.
 */
trait ReplaceWith extends IVm {

  override type T <: Node

  def replaceWith(newVm: IVm { type T <: Node }): Unit = {
    val parent = _underlying.parentNode
    parent.replaceChild(newVm._underlying, _underlying)
  }

}
