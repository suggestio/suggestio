package io.suggest.sc.sjs.vm.nav.nodelist

import io.suggest.sc.sjs.m.mnav.NodeListClick
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sc.sjs.vm.nav.nodelist.glay.GlayRoot
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.ScConstants.NavPane.{GN_CONTAINER_ID, GNL_BODY_CSS_CLASS, GN_ATTR_LAYERS_COUNT}
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.NodeList
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


trait NlContainerT extends SafeElT with InitOnClickToFsmT {

  override type T = HTMLDivElement

  /** Выдать все html-теги с телами GNL-тегов. */
  def allGnlTags: NodeList = {
    // TODO Opt А можно ведь искать(фильтровать) только child-элементы, а не гулять по всему поддереву.
    _underlying.getElementsByClassName(GNL_BODY_CSS_CLASS)
  }
  
  def gnLayerBodiesIter = {
    DomListIterator(allGnlTags)
      .map { node =>
        GlayRoot( node.asInstanceOf[HTMLDivElement] )
      }
  }

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
  override lazy val allGnlTags = super.allGnlTags
  override lazy val layersCount = super.layersCount
}
