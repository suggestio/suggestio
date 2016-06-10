package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.{ChildrenVms, OfDiv}
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdRawString, OfHtmlElDomIdRelated}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 15:43
 * Description: Тело города, т.е. заголовки вкладок групп узлов.
 */
object CityTabs extends FindElDynIdT with OfDiv with DynDomIdRawString with DomIdPrefixed with OfHtmlElDomIdRelated {

  override type Dom_t         = HTMLDivElement
  override type DomIdArg_t    = String
  override type T             = CityTabs
  override def DOM_ID_PREFIX  = AdvDirectFormConstants.CITY_TAB_BODY_PREFIX

}


import CityTabs.Dom_t


trait CityTabsT extends CityIdT with ShowHideDisplayT with SetIsShown with ChildrenVms {

  override type T = Dom_t

  override type ChildVm_t = CityCatTab
  override protected def _childVmStatic = CityCatTab

  def tabHeads = _childrenVms

}


case class CityTabs(override val _underlying: Dom_t)
  extends CityTabsT
