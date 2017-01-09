package io.suggest.lk.adv.geo.tags.vm.popup.rcvr

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.of.{ChildrenVms, OfDiv}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.16 21:28
  * Description: vm'ка для внешнего div'а попапа, который содержит в себе всё остальное.
  */
object Outer extends OfDiv {
  override type T = Outer
}


import Outer.Dom_t

case class Outer(override val _underlying: Dom_t) extends VmT with ChildrenVms {

  override type T = Dom_t

  override type ChildVm_t = NodesGroup
  override protected def _childVmStatic = NodesGroup

  /** Итератор по vm'кам групп узлов. */
  def groupsIter = _childrenVms

}
