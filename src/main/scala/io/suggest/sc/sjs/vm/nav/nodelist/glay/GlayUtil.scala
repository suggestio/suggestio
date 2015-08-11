package io.suggest.sc.sjs.vm.nav.nodelist.glay

import io.suggest.sc.ScConstants.NavPane.{GNL_BODY_DIV_ID_PREFIX, GNL_ATTR_LAYER_ID_INDEX}
import io.suggest.sc.sjs.vm.util.domvm.{IApplyEl, FindElIndexedIdT}
import io.suggest.sc.sjs.vm.util.domvm.get.ISubTag
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.attr.SafeAttrElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 11:57
 * Description: Утиль для сборки vm'ок, относящихся к подспискам узлов списка навигационной панели.
 */
trait GlayDivStaticT extends FindElIndexedIdT {

  override type Dom_t = HTMLDivElement
  override def DOM_ID: String = GNL_BODY_DIV_ID_PREFIX

}


/** [[GlayDivStaticT]] с поддержкой суффиксов. */
trait GlayDivStaticSuffixedT extends GlayDivStaticT {

  protected def _DOM_ID_SUFFIX: String

  override def getDomId(arg: Int): String = {
    super.getDomId(arg) + _DOM_ID_SUFFIX
  }
}


/** Трейт для сборки классов vm'ок, имеющих быстрый доступ к нижележащим элементам. */
trait GlayT extends SafeElT with ISubTag {

  override type T = HTMLDivElement

  protected def _subtagCompanion: IApplyEl { type T = SubTagVm_t; type Dom_t = HTMLDivElement }

  protected def _findSubtag(): Option[SubTagVm_t] = {
    val el = _underlying.firstChild.asInstanceOf[HTMLDivElement]
    val opt = Option( el )
    opt.map { _subtagCompanion.apply }
  }

}


/** API для чтения значения из аттрибута data-index="1". */
trait LayerIndex extends SafeAttrElT {
  def layerIndexOpt = getIntAttributeStrict(GNL_ATTR_LAYER_ID_INDEX)
  def layerIndex    = layerIndexOpt.get
}
