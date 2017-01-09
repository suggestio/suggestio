package io.suggest.sc.sjs.vm.nav.nodelist.glay

import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.DIV_WRAPPER_SUFFIX

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 11:48
 * Description: wrapper div для подсписка узлов в рамках геослоя в списке узлов на панели навигации.
 */
object GlayWrapper extends GlayDivStaticSuffixedT {
  override type T = GlayWrapper
  override protected def _DOM_ID_SUFFIX = DIV_WRAPPER_SUFFIX
}


trait GlayWrapperT extends GlayT {
  override type T = HTMLDivElement
  override type SubTagVm_t = GlayContent
  override protected def _subtagCompanion = GlayContent

  def content = _findSubtag()
}


case class GlayWrapper(
  override val _underlying: HTMLDivElement
)
  extends GlayWrapperT
