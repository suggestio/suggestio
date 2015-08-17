package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.ScConstants.DIV_CONTENT_SUFFIX
import io.suggest.sc.sjs.vm.util.domvm.get.ContentElT
import io.suggest.sc.sjs.vm.util.domvm.{FindElIndexedIdT, IndexedSuffixedDomId}
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:21
 * Description: Content-div focused-карточки.
 */
object FAdContent extends FindElIndexedIdT with IndexedSuffixedDomId with FAdStatic {
  override protected def DOM_ID_SUFFIX = DIV_CONTENT_SUFFIX
  override type T = FAdContent
}


trait FAdContentT extends SafeElT with ContentElT {
  override type T = HTMLDivElement
}


case class FAdContent(
  override val _underlying: HTMLDivElement
)
  extends FAdContentT
