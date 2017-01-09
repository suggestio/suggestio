package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.adv.direct.AdvDirectFormConstants.CityNgIdOpt
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.input.{CheckBoxVmStaticT, CheckBoxVmT}
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdToString, OfHtmlElDomIdRelated}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.16 14:01
 * Description: VM'ка для чекбокса на уровне таба.
 */
object TabCheckBox
  extends FindElDynIdT
    with CheckBoxVmStaticT
    with DynDomIdToString
    with DomIdPrefixed
    with OfHtmlElDomIdRelated
{

  override type DomIdArg_t    = CityNgIdOpt
  override type T             = TabCheckBox
  override def DOM_ID_PREFIX  = AdvDirectFormConstants.CITY_NODES_TAB_HEAD_CHECKBOX_ID_PREFIX

}


import TabCheckBox.Dom_t


case class TabCheckBox(override val _underlying: Dom_t)
  extends CheckBoxVmT
{

  def tabHead = CityCatTab.ofNodeUp(_underlying.parentNode)

}

