package io.suggest.lk.adv.direct.vm.nbar.nodes

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.OfDiv
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 20:42
 * Description: vm'ка для одного узла в списке узлов города.
 */
object NodeRow extends FindElDynIdT with OfDiv {

  override type DomIdArg_t  = String
  override type Dom_t       = HTMLDivElement
  override type T           = NodeRow

  override def getDomId(nodeId: DomIdArg_t): String = {
    AdvDirectFormConstants.NODE_ROW_ID(nodeId)
  }

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      el.id.startsWith( AdvDirectFormConstants.NODE_ROW_ID_PREFIX )
    }
  }

}


import NodeRow.Dom_t


trait NodeRowT extends IVm {
  override type T = Dom_t
}


case class NodeRow(override val _underlying: Dom_t)
  extends NodeRowT
