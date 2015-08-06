package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sc.sjs.vm.search.tabs.{TabContentCompanion, TabContent}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.Search.Nodes.CONTENT_DIV_ID
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 14:25
 * Description: VM div'а контейнера контента вкладки выбора хеш-тегов
 */
object ShtContent extends TabContentCompanion {

  override type T = ShtContent

  override def DOM_ID = CONTENT_DIV_ID

}


trait ShtContentT extends TabContent


case class ShtContent(
  override val _underlying: HTMLDivElement
)
  extends ShtContentT
