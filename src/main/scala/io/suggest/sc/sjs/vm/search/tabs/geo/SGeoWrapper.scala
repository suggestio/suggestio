package io.suggest.sc.sjs.vm.search.tabs.geo

import io.suggest.sc.sjs.vm.search.tabs.{TabWrapper, TabWrapperCompanion}
import io.suggest.sc.sjs.vm.util.domvm.get.SubTagFind
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.Cats.WRAPPER_DIV_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 17:21
 * Description: vm'ка для wrapper div в search-табе геопоиска.
 */
object SGeoWrapper extends TabWrapperCompanion {
  override type T = SGeoWrapper
  override def DOM_ID = WRAPPER_DIV_ID
}


trait SGeoWrapperT extends SubTagFind with TabWrapper {

  override protected type SubtagCompanion_t = SGeoContent.type
  override type SubTagVm_t                = SGeoContent.T
  override protected def _subtagCompanion = SGeoContent
  override protected type SubTagEl_t      = SGeoContent.Dom_t
}


case class SGeoWrapper(
  override val _underlying: HTMLDivElement
)
  extends SGeoWrapperT
