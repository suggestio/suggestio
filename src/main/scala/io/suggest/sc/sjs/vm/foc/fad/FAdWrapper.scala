package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.sjs.vm.util.domvm.get.WrapperChildContent
import io.suggest.sc.sjs.vm.util.domvm._
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.DIV_WRAPPER_SUFFIX

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 18:15
 * Description: Враппер-div focused-карточки.
 */
object FAdWrapper extends FindElIndexedIdT with IndexedSuffixedDomId with FAdStatic {
  override protected def DOM_ID_SUFFIX = DIV_WRAPPER_SUFFIX
  override type T = FAdWrapper
}


trait FAdWrapperT extends _FAdFindSubtag with WrapperChildContent {
  override type SubTagVm_t = FAdContent.T
  override protected type SubTagEl_t = FAdContent.Dom_t
  override protected type SubtagCompanion_t = FAdContent.type
  override protected def _subtagCompanion = FAdContent
}


case class FAdWrapper(
  override val _underlying: HTMLDivElement
)
  extends FAdWrapperT
{
  override lazy val content = super.content
}
