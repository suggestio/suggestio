package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sc.ScConstants.NavPane.{SCREEN_OFFSET, GNL_DOM_HEIGHT, GNL_BODY_HIDDEN_CSS_CLASS}
import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.v.vutil.VUtil
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


trait GlayRootT extends GlayT with LayerIndex {

  override type T = HTMLDivElement

  override protected def _subtagCompanion = GlayWrapper
  override type SubTagVm_t = GlayWrapper

  /** Скрыто ли тело текущего геослоя? */
  def isHidden: Boolean = {
    containsClass(GNL_BODY_HIDDEN_CSS_CLASS)
  }

  /** Скрыть тело текущего гео-слоя. */
  def hide(): Unit = {
    addClasses(GNL_BODY_HIDDEN_CSS_CLASS)
  }
  /** Отобразить на экран тело текущего гео-слоя. */
  def show(): Unit = {
    removeClass(GNL_BODY_HIDDEN_CSS_CLASS)
  }

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

  def caption(): Option[GlayCaption] = {
    for (node <- Option( _underlying.previousSibling )) yield {
      GlayCaption( node.asInstanceOf[HTMLDivElement] )
    }
  }

}


case class GlayRoot(
  override val _underlying: HTMLDivElement
) extends GlayRootT {

  override lazy val layerIndex = super.layerIndex

}
