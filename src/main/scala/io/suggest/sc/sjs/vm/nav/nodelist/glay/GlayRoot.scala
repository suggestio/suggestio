package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sc.ScConstants.NavPane.{GNL_ATTR_LAYER_ID_INDEX, SCREEN_OFFSET, GNL_DOM_HEIGHT}
import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.v.vutil.VUtil
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

  override def isHidden = super.isHidden    // protected -> public

  def wrapper = _findSubtag()
  // TODO Нужно заимплентить метод fixHeightForGnlExpanded.

  /**
   * Поправить высоту сегмента под экран.
   * Раньше логика метода обитала в NavPanelCtl.fixHeightForGnlExpanded().
   * @param screen экран.
   * @param layersCount кол-во слоёв.
   */
  def fixHeightExpanded(screen: IMScreen, layersCount: Int): Unit = {
    for {
      _wrapper <- wrapper
      _content <- _wrapper.content
    } {
      val domH = _underlying.offsetHeight.toInt
      val maxH = screen.height - SCREEN_OFFSET - (layersCount + 1) * GNL_DOM_HEIGHT
      val targetH = Math.min(maxH, domH)
      var containers = List(_wrapper._underlying)
      if (domH > maxH)
        containers ::= _underlying
      VUtil.setHeightRootWrapCont(targetH, Some(_content._underlying), containers)
    }
  }

}


case class GlayRoot(
  override val _underlying: HTMLDivElement
) extends GlayRootT {

  override lazy val layerIndex = super.layerIndex

}
