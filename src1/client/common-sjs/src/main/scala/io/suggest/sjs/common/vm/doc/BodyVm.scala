package io.suggest.sjs.common.vm.doc

import io.suggest.sjs.common.vm.{IVm, VmT}
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLBodyElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 18:12
  */

object SafeBody extends VmT {

  override type T = HTMLBodyElement

  override def _underlying = DocumentVm().body

  def append(vm: IVm { type T <: Node }): Unit = {
    _underlying.appendChild( vm._underlying )
  }

}
