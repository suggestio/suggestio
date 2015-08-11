package io.suggest.sc.sjs.vm.nav.nodelist

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.NavPane.WRAPPER_ID
import io.suggest.sc.sjs.vm.util.domvm.get.WrapperChildContent
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 15:07
 * Description: VM враппера списка узлов на панели навигации.
 */
object NlWrapper extends FindDiv {
  override type T = NlWrapper
  override def DOM_ID = WRAPPER_ID
}


trait NlWrapperT extends WrapperChildContent {
  override type T = HTMLDivElement

  override protected type SubTagEl_t      = NlContent.Dom_t
  override type SubTagVm_t                = NlContent.T
  override protected def _subtagCompanion = NlContent

}


case class NlWrapper(
  override val _underlying: HTMLDivElement
)
  extends NlWrapperT
{
  override lazy val content = super.content
}
