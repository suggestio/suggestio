package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sc.ScConstants.NavPane.GNL_ATTR_LAYER_ID_INDEX
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 18:20
 * Description: vm тела гео-слоя в гео-списке узлов.
 */

object GlayRoot extends GlayDivStaticT {
  override type Dom_t = HTMLDivElement
  override type T     = GlayRoot
}


trait GlayRootT extends GlayT with SetDisplayEl {

  override type T = HTMLDivElement

  override protected def _subtagCompanion = GlayWrapper
  override type SubTagVm_t = GlayWrapper

  def layerIndex = getIntAttributeStrict(GNL_ATTR_LAYER_ID_INDEX).get
  def id = _underlying.id

  override def isHidden = super.isHidden
  def wrapper = _findSubtag()
  // TODO Нужно заимплентить метод fixHeightForGnlExpanded.
}


case class GlayRoot(
  override val _underlying: HTMLDivElement
) extends GlayRootT {

  override lazy val layerIndex = super.layerIndex

}
