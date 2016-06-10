package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.{ChildrenVms, OfDiv}
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdRawString, OfHtmlElDomIdRelated}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 19:19
 * Description: Контейнер всех [[CityCatNg]] в рамках города.
 */
object CityNgs
  extends FindElDynIdT
    with OfDiv
    with DynDomIdRawString
    with DomIdPrefixed
    with OfHtmlElDomIdRelated
{

  override type DomIdArg_t    = String
  override type Dom_t         = HTMLDivElement
  override type T             = CityNgs
  override def DOM_ID_PREFIX  = AdvDirectFormConstants.NGRPS_CITY_CONT_ID_PREFIX

}


import CityNgs.Dom_t


trait CityNgsT extends CityIdT with ShowHideDisplayT with ChildrenVms {

  override type T = Dom_t
  override type ChildVm_t = CityCatNg

  override protected def _childVmStatic = CityCatNg

  def nodeGroups  = _childrenVms

}


case class CityNgs(override val _underlying: Dom_t)
  extends CityNgsT
