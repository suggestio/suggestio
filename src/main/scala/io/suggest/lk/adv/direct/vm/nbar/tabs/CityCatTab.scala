package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.m.CityNgIdOpt
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.lk.adv.direct.vm.nbar.ngroups.NgIdT
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.OfDiv
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 17:41
 * Description: Заголовок одной вкладки.
 */
object CityCatTab extends FindElDynIdT with OfDiv {

  override type Dom_t       = HTMLDivElement
  override type T           = CityCatTab
  override type DomIdArg_t  = CityNgIdOpt

  override def getDomId(cityNgIdOpt: DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_NODES_TAB_HEAD_ID(cityNgIdOpt.cityId, cityNgIdOpt.ngIdOpt)
  }

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      el.id.startsWith( AdvDirectFormConstants.CITY_NODES_TAB_HEAD_ID_PREFIX )
    }
  }

}


import CityCatTab.Dom_t


trait CityCatTabT extends IVm with CityIdT with NgIdT {

  override type T = Dom_t

  def checkBox = {
    cityId.flatMap { _cityId =>
      val arg = CityNgIdOpt(cityId.get, ngId)
      TabCheckBox.find( arg )
    }
  }

}


case class CityCatTab(override val _underlying: Dom_t)
  extends CityCatTabT
