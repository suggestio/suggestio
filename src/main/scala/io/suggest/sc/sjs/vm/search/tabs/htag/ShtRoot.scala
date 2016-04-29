package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sc.sjs.vm.search.tabs.{TabRoot, TabRootCompanion}
import io.suggest.sjs.common.vm.child.SubTagFind
import io.suggest.sjs.common.vm.util.IInitLayout
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.Nodes.ROOT_DIV_ID
import io.suggest.sc.sjs.m.msearch.MTabs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 13:41
 * Description: VM корневого контейнера тела вкладки с поисковыми хеш-тегами.
 */
object ShtRoot extends TabRootCompanion {
  override type T     = ShtRoot
  override def DOM_ID = ROOT_DIV_ID
}


trait ShtRootT extends SubTagFind with TabRoot {

  override protected type SubtagCompanion_t = ShtWrapper.type
  override type SubTagVm_t                  = ShtWrapper.T
  override protected type SubTagEl_t        = ShtWrapper.Dom_t
  override protected def _subtagCompanion   = ShtWrapper
  override def mtab   = MTabs.Tags

  override def initLayout(): Unit = {
    val f = IInitLayout.f
    // Инициализация списка тегов/узлов.
    StList.find().foreach(f)
  }

}


case class ShtRoot(
  override val _underlying: HTMLDivElement
)
  extends ShtRootT
{

  override lazy val wrapper = super.wrapper

}
