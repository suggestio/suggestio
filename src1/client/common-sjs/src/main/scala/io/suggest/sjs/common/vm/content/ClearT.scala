package io.suggest.sjs.common.vm.content

import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.08.15 13:39
 * Description: Поддержка безопасной очистки тела элемента.
 */
object ClearT {

  def f = { x: ClearT => x.clear() }

}


trait ClearT extends IVm {

  override type T <: Node

  def clear(): Unit = {
    VUtil.removeAllChildren(_underlying)
  }

}
