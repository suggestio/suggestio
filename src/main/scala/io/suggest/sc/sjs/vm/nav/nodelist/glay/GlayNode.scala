package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.{FindElDynIdT, FindFromChildByClass}
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.NavPane.{GNL_NODE_ID_PREFIX, GN_ATTR_NODE_ID, GN_NODE_CSS_CLASS}
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdToString}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 14:13
 * Description: vm'ка для div узла в списке узлов панели навигации.
 */
object GlayNode
  extends FindElDynIdT
    with FindFromChildByClass
    with DynDomIdToString
    with DomIdPrefixed
{

  override type Dom_t = HTMLDivElement
  override type T     = GlayNode
  override def DOM_ID_PREFIX = GNL_NODE_ID_PREFIX

  override protected def _findParentCssMarker = GN_NODE_CSS_CLASS

}


trait GlayNodeT extends VmT {

  override type T = HTMLDivElement

  def adnIdOpt = getAttribute(GN_ATTR_NODE_ID)
  def adnId = adnIdOpt.get

}


case class GlayNode(
  override val _underlying: HTMLDivElement
)
  extends GlayNodeT
