package io.suggest.lk.adv.geo.tags.vm.popup.rcvr

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.of.OfDiv

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.16 21:37
  * Description: Контейнер инпутов одного узла.
  */
object NodeDiv extends OfDiv {
  override type T = NodeDiv
}

import NodeDiv.Dom_t

case class NodeDiv(override val _underlying: Dom_t) extends VmT {
  override type T = Dom_t
}
