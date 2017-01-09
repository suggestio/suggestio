package io.suggest.sjs.common.vm.rm

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:23
 * Description: Аддон для поддержки самоудаления узла.
 */
trait SelfRemoveT extends IVm {

  override type T <: Node

  def remove(): Unit = {
    val n = _underlying
    n.parentNode.removeChild(n)
  }

}
