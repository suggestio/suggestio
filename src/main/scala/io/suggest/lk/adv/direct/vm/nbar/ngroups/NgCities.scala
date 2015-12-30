package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.fsm.{SjsFsm, IInitLayoutFsm}
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.evtg.OnMouseClickT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.input.CheckBoxVm
import io.suggest.sjs.common.vm.util.IInitLayout
import org.scalajs.dom.Event
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 19:36
 * Description: Контейнер всех [[NgBodiesCity]] во всех городах.
 */
object NgCities extends FindDiv {
  override type T     = NgCities
  override def DOM_ID = AdvDirectFormConstants.NGRPS_CONT_ID
}


import io.suggest.lk.adv.direct.vm.nbar.ngroups.NgCities.Dom_t


trait NgCitiesT extends OnMouseClickT with IInitLayoutFsm {

  override type T = Dom_t

  def cities: Iterator[NgBodiesCity] = {
    for (el <- DomListIterator( _underlying.children )) yield {
      val el1 = el.asInstanceOf[ NgBodiesCity.Dom_t ]
      NgBodiesCity(el1)
    }
  }



  override def initLayout(fsm: SjsFsm): Unit = {
    onClick { event: Event =>
      val el = event.target.asInstanceOf[HTMLElement]
      if (el.tagName.equalsIgnoreCase("INPUT")) {
        val el2 = el.asInstanceOf[HTMLInputElement]
        if (el2.`type`.equalsIgnoreCase("CHECKBOX")) {
          val vm2 = CheckBoxVm(el2)
          vm2._underlying.value = vm2.isChecked.toString
        }
      }
    }
  }

}


case class NgCities(override val _underlying: Dom_t)
  extends NgCitiesT
