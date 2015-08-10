package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.DIV_CONTENT_SUFFIX

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 11:53
 * Description: content div подсписка узлов геослоя в рамках списка узлов панели навигации.
 */
object GlayContent extends GlayDivStaticSuffixedT {
  override type T = GlayContent
  override protected def _DOM_ID_SUFFIX = DIV_CONTENT_SUFFIX
}


trait GlayContentT extends SafeElT {
  override type T = HTMLDivElement
}


case class GlayContent(
  override val _underlying: HTMLDivElement
)
  extends GlayContentT
