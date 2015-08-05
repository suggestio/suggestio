package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sc.ScConstants.Search.Nodes.WRAPPER_DIV_ID
import io.suggest.sc.sjs.vm.search.tabs.TabWrapper
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 14:22
 * Description: VM для wrapper div'а вкладки хеш-тегов панели поиска.
 */
object ShtWrapper extends FindDiv {

  override type T = ShtWrapper

  /** id элемента в рамках DOM. */
  override def DOM_ID = WRAPPER_DIV_ID

}


trait ShtWrapperT extends TabWrapper {

  override type T = HTMLDivElement

  override type SubTagVm_t = ShtContent.T
  override protected type SubTagEl_t = ShtContent.Dom_t
  override protected def _subtagCompanion = ShtContent

}


case class ShtWrapper(
  override val _underlying: HTMLDivElement
)
  extends ShtWrapperT
{

  override lazy val content = super.content

}
