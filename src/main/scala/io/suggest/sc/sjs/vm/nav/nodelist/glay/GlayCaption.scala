package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sc.sjs.vm.util.FindVmForCss
import io.suggest.sc.sjs.vm.util.domvm.FindElIndexedIdT
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.NavPane.{GNL_CAPTION_DIV_ID_PREFIX, GNL_CAPTION_CSS_CLASS, GNL_ACTIVE_CSS_CLASS}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 14:57
 * Description: vm для div'а заголовка геослоя.
 */
object GlayCaption extends FindElIndexedIdT with FindVmForCss  {
  override type Dom_t = HTMLDivElement
  override type T     = GlayCaption
  override def DOM_ID = GNL_CAPTION_DIV_ID_PREFIX

  override protected def _findParentCssMarker = GNL_CAPTION_CSS_CLASS
}


trait GlayCaptionT extends SafeElT with LayerIndex with GlayContainerT {

  override type T = HTMLDivElement

  /** Деактивация заголовка геослоя на экране. */
  def activate(): Unit = {
    addClasses(GNL_ACTIVE_CSS_CLASS)
  }

  /** Активация заголовка геослоя на экране. */
  def deactivate(): Unit = {
    removeClass(GNL_ACTIVE_CSS_CLASS)
  }

  def body: Option[GlayRoot] = {
    for (node <- Option( _underlying.nextSibling )) yield {
      GlayRoot( node.asInstanceOf[HTMLDivElement] )
    }
  }

}


case class GlayCaption(
  override val _underlying: HTMLDivElement
)
  extends GlayCaptionT
