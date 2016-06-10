package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.adv.direct.AdvDirectFormConstants.CityNgIdOpt
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.OfDiv
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdToString, OfHtmlElDomIdRelated}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 17:41
 * Description: Заголовок одной вкладки.
 */
object CityCatTab
  extends FindElDynIdT
    with OfDiv
    with DynDomIdToString
    with DomIdPrefixed
    with OfHtmlElDomIdRelated
{

  override type Dom_t         = HTMLDivElement
  override type T             = CityCatTab
  override type DomIdArg_t    = CityNgIdOpt
  override def DOM_ID_PREFIX  = AdvDirectFormConstants.CITY_NODES_TAB_HEAD_ID_PREFIX

}


import CityCatTab.Dom_t


trait CityCatTabT extends IVm with WithCityNgIdOpt {

  override type T = Dom_t

  def checkBox = _findByCityNgIdOpt(TabCheckBox)
  def counter  = _findByCityNgIdOpt(TabCounter)

}


case class CityCatTab(override val _underlying: Dom_t)
  extends CityCatTabT
