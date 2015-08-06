package io.suggest.sc.sjs.vm.search.tabs.geo

import io.suggest.sc.sjs.vm.search.tabs.{TabContentCompanion, TabContent}
import io.suggest.sc.ScConstants.Search.Cats.CONTENT_DIV_ID
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 17:39
 * Description: vm content div для гео-вкладки панели поиска.
 */
object SGeoContent extends TabContentCompanion {
  override type T     = SGeoContent
  override def DOM_ID = CONTENT_DIV_ID
}


trait SGeoContentT extends TabContent


case class SGeoContent(
  override val _underlying: HTMLDivElement
)
  extends SGeoContentT
