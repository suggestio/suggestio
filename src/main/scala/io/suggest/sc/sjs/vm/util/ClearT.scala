package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.08.15 13:39
 * Description: Поддержка безопасной очистки тела элемента.
 */
trait ClearT extends ISafe {

  override type T <: Node

  def clear(): Unit = {
    VUtil.removeAllChildren(_underlying)
  }

}
