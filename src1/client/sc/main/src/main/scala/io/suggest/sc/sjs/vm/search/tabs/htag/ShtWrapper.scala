package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sc.ScConstants.Search.TagsTab.WRAPPER_DIV_ID
import io.suggest.sc.sjs.vm.search.tabs.{TabWrapperCompanion, TabWrapper}
import io.suggest.sjs.common.vm.child.SubTagFind
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 14:22
 * Description: VM для wrapper div'а вкладки хеш-тегов панели поиска.
 */
object ShtWrapper extends TabWrapperCompanion {

  override type T = ShtWrapper
  override def DOM_ID = WRAPPER_DIV_ID

}


trait ShtWrapperT extends SubTagFind with TabWrapper {

  override protected type SubtagCompanion_t = ShtContent.type
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
