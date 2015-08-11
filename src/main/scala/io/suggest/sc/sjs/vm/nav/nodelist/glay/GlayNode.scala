package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sc.sjs.vm.util.FindVmForCss
import io.suggest.sc.sjs.vm.util.domvm.FindElIndexedIdT
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.NavPane.{GNL_NODE_ID_PREFIX, GN_NODE_CSS_CLASS, GN_ATTR_NODE_ID}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 14:13
 * Description: vm'ка для div узла в списке узлов панели навигации.
 */
object GlayNode extends FindElIndexedIdT with FindVmForCss {

  override type Dom_t = HTMLDivElement
  override type T     = GlayNode
  override def DOM_ID = GNL_NODE_ID_PREFIX

  override protected def _findParentCssMarker = GN_NODE_CSS_CLASS

}


trait GlayNodeT extends SafeElT {

  override type T = HTMLDivElement

  def adnIdOpt = getAttribute(GN_ATTR_NODE_ID)
  def adnId = adnIdOpt.get

}


case class GlayNode(
  override val _underlying: HTMLDivElement
)
  extends GlayNodeT
