package io.suggest.lk.adv.direct.vm.nodes

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindDiv

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 18:54
 * Description: Корневой контейнер системы выбора узлов.
 */
object Root extends FindDiv {
  override type T     = Root
  override def DOM_ID = AdvDirectFormConstants.NODES_BAR_ID
}


import io.suggest.lk.adv.direct.vm.nodes.Root.Dom_t


trait RootT extends IVm {
  override type T = Dom_t
}


case class Root(override val _underlying: Dom_t)
  extends RootT
