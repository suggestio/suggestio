package io.suggest.sc.sjs.vm.search.tabs.geo

import io.suggest.sc.sjs.vm.search.tabs.{TabRoot, TabRootCompanion}
import io.suggest.sc.ScConstants.Search.Cats.ROOT_DIV_ID
import io.suggest.sc.sjs.vm.util.domvm.get.SubTagFind
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 17:13
 * Description: vm'ка доступа к основному div'у вкладки географии узлов.
 */
object SGeoRoot extends TabRootCompanion {
  override type T = SGeoRoot
  override def DOM_ID: String = ROOT_DIV_ID
}


trait SGeoRootT extends SubTagFind with TabRoot {

  override protected type SubtagCompanion_t = SGeoWrapper.type
  override type SubTagVm_t                  = SGeoWrapper.T
  override protected def _subtagCompanion   = SGeoWrapper
  override protected type SubTagEl_t        = SGeoWrapper.Dom_t

  /** Инициализация текущей и подчиненных ViewModel'ей. */
  override def initLayout(): Unit = {
    dom.console.warn("TODO SgeoRoot.initLayout() not implemented.")
  }

}


case class SGeoRoot(
  override val _underlying: HTMLDivElement
)
  extends SGeoRootT
