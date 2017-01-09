package io.suggest.lk.adv.direct.vm.nbar.tabs

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.m.NgClick
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.evtg.OnMouseClickT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.of.ChildrenVms
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 15:31
 * Description: "Тела" городов, т.е. заголовки групп узлов в рамках городов.
 */
object CitiesTabs extends FindDiv {
  override type T     = CitiesTabs
  override def DOM_ID = AdvDirectFormConstants.CITIES_BODIES_CONT_ID
}


import CitiesTabs.Dom_t


trait CitiesTabsT extends VmT with ShowHideDisplayT with IInitLayoutFsm with OnMouseClickT with ChildrenVms {

  override type T = Dom_t

  override type ChildVm_t = CityTabs
  override protected def _childVmStatic = CityTabs

  def bodies = _childrenVms

  /** Инициализация поддержки событий. */
  override def initLayout(fsm: SjsFsm): Unit = {
    onClick { event: Event =>
      for (ngHead <- CityCatTab.ofEventTargetUp( event.target )) {
        fsm !! NgClick(event, ngHead)
      }
    }
  }

}


case class CitiesTabs(override val _underlying: Dom_t)
  extends CitiesTabsT
