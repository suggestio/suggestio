package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.IApplyEl
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.Nodes.ATTR_NODE_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.09.15 17:38
 * Description: vm'ка одного ряда в списке тегов.
 */
object StListRow extends IApplyEl {
  override type T = StListRow
  override type Dom_t = HTMLDivElement
}


trait StListRowT extends VmT {

  override type T = HTMLDivElement

  def nodeId = getAttribute(ATTR_NODE_ID)

}

case class StListRow(
  override val _underlying: HTMLDivElement
)
  extends StListRowT
