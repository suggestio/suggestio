package io.suggest.lk.adv.geo.tags.vm.popup.rcvr

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.of.OfDiv

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.16 21:32
  * Description: VM'ка для div'а, который занимается хранением внутри себя группы узлов.
  */
object NodesGroup extends OfDiv {
  override type T = NodesGroup
}

import NodesGroup.Dom_t

case class NodesGroup(override val _underlying: Dom_t) extends VmT {

  override type T = Dom_t

}
