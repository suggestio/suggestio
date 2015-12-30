package io.suggest.lk.adv.direct.vm.nbar.cities

import io.suggest.adv.direct.AdvDirectFormConstants.{CITIES_HEADS_CONT_ID, CITY_TAB_HEAD_CLASS}
import io.suggest.lk.adv.direct.m.CityTabHeadClick
import io.suggest.lk.slide.block.vm.SbContent
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.evtg.OnMouseClickT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import io.suggest.sjs.common.vm.{Vm, VmT}
import org.scalajs.dom.{Event, Node}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 13:43
 * Description: Контейнер заголовков городов.
 * Переключение формы между городами происходит отсюда.
 */
object CitiesHeads extends FindDiv {
  override type T     = CitiesHeads
  override def DOM_ID = CITIES_HEADS_CONT_ID
}


import io.suggest.lk.adv.direct.vm.nbar.cities.CitiesHeads.Dom_t


trait CitiesHeadsT extends VmT with IInitLayoutFsm with OnMouseClickT with ShowHideDisplayT {

  override type T = Dom_t

  /** Инициализация событий списка городов. */
  override def initLayout(fsm: SjsFsm): Unit = {
    onClick { event: Event =>
      val vm = Vm( event.target.asInstanceOf[Node] )
      for (vm1 <- VUtil.hasCssClass(vm, CITY_TAB_HEAD_CLASS)) {
        val vm2 = CityTabHead( vm1._underlying.asInstanceOf[CityTabHead.Dom_t] )
        fsm !! CityTabHeadClick(event, vm2)
      }
    }
  }

  override def hide(): Unit = {
    for (sbc <- SbContent.of(this)) {
      sbc.hide()
    }
  }

}


case class CitiesHeads(override val _underlying: Dom_t)
  extends CitiesHeadsT
