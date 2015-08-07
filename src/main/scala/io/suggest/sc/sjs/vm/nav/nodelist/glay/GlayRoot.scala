package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sc.ScConstants.NavPane.GNL_ATTR_LAYER_ID_INDEX
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 18:20
 * Description: vm тела гео-слоя в гео-списке узлов.
 */

trait GlayBodyT extends SafeElT with SetDisplayEl {
  override type T = HTMLDivElement

  def layerIndex = getIntAttributeStrict(GNL_ATTR_LAYER_ID_INDEX).get
  def id = _underlying.id

  override def isHidden = super.isHidden

  // TODO Нужно заимплентить vm'ки для wrapper, content, container. И метод fixHeightForGnlExpanded.
}

case class GlayRoot(
  override val _underlying: HTMLDivElement
) extends GlayBodyT {

  override lazy val layerIndex = super.layerIndex

}
