package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.vm.nbar.cities.CityIdT
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.of.OfDiv
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 15:43
 * Description: Тело города, т.е. заголовки вкладок групп узлов.
 */
object CityTabs extends FindElDynIdT with OfDiv {

  override type Dom_t       = HTMLDivElement
  override type DomIdArg_t  = String
  override type T           = CityTabs

  override def getDomId(cityId: DomIdArg_t): String = {
    AdvDirectFormConstants.CITY_TAB_BODY_ID(cityId)
  }

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      el.id.startsWith( AdvDirectFormConstants.CITY_TAB_BODY_PREFIX )
    }
  }

}


import CityTabs.Dom_t


trait CityTabsT extends CityIdT with ShowHideDisplayT with SetIsShown {

  override type T = Dom_t

  def ngHeads: Iterator[CityCatTab] = {
    for (el <- DomListIterator( _underlying.children )) yield {
      val el1 = el.asInstanceOf[CityCatTab.Dom_t]
      CityCatTab(el1)
    }
  }

}


case class CityTabs(override val _underlying: Dom_t)
  extends CityTabsT
