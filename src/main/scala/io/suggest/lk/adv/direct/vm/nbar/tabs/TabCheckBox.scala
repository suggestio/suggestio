package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.m.CityNgIdOpt
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.input.{CheckBoxVmT, CheckBoxVmStaticT}
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.16 14:01
 * Description: VM'ка для чекбокса на уровне таба.
 */
object TabCheckBox extends FindElDynIdT with CheckBoxVmStaticT {

  override type DomIdArg_t  = CityNgIdOpt
  override type T           = TabCheckBox

  override def getDomId(cnio: DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_NODES_TAB_HEAD_CHECKBOX_ID(cnio.cityId, cnio.ngIdOpt)
  }

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      el.id.startsWith( AdvDirectFormConstants.CITY_NODES_TAB_HEAD_CHECKBOX_ID_PREFIX )
    }
  }

}


import TabCheckBox.Dom_t


case class TabCheckBox(override val _underlying: Dom_t)
  extends CheckBoxVmT
{

  def tabHead = CityCatTab.ofNodeUp(_underlying.parentNode)

}

