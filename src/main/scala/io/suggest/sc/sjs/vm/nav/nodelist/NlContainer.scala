package io.suggest.sc.sjs.vm.nav.nodelist

import io.suggest.sc.sjs.m.mnav.NodeListClick
import io.suggest.sc.sjs.vm.util.InitOnClickToScFsmT
import io.suggest.sc.sjs.vm.nav.nodelist.glay.GlayRoot
import io.suggest.sc.ScConstants.NavPane.{GN_ATTR_LAYERS_COUNT, GN_CONTAINER_ID}
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.child.ChildsByClassName
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 18:02
 * Description: vm для динамического div'а со списком узлов.
 * Список запрашивается при обращении куда надо.
 */
object NlContainer extends FindDiv {
  override type T = NlContainer
  override def DOM_ID = GN_CONTAINER_ID
}


trait NlContainerT extends VmT with InitOnClickToScFsmT with ChildsByClassName {

  override type T = HTMLDivElement

  def gnLayerBodiesIter = _findChildsByClass(GlayRoot)

  /** Поиск первого развернутого геослоя. */
  def findFirstExpanded: Option[GlayRoot] = {
    gnLayerBodiesIter
      .filter { gnl =>
        !gnl.isHidden
      }
      .toStream
      .headOption
  }

  /** Прочитать кол-во гео-слоёв из соотв.аттрибута. */
  def layersCount: Int = {
    getIntAttributeStrict(GN_ATTR_LAYERS_COUNT).get
  }

  override protected[this] def _clickMsgModel = NodeListClick
}


case class NlContainer(
  override val _underlying: HTMLDivElement
)
  extends NlContainerT
{
  override lazy val layersCount = super.layersCount
}
