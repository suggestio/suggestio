package io.suggest.lk.adv.direct.vm.nbar.cities

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.m.NgClick
import io.suggest.lk.adv.direct.vm.nbar.ngroups.NgHead
import io.suggest.sjs.common.fsm.{SjsFsm, IInitLayoutFsm}
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.{Vm, VmT}
import io.suggest.sjs.common.vm.evtg.OnMouseClickT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import org.scalajs.dom.{Node, Event}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 15:31
 * Description: "Тела" городов, т.е. заголовки групп узлов в рамках городов.
 */
object CityBodies extends FindDiv {
  override type T     = CityBodies
  override def DOM_ID = AdvDirectFormConstants.CITIES_BODIES_CONT_ID
}


import CityBodies.Dom_t


trait CityBodiesT extends VmT with ShowHideDisplayT with IInitLayoutFsm with OnMouseClickT {

  override type T = Dom_t

  def bodies: Iterator[CityBody] = {
    for (el <- DomListIterator(_underlying.children)) yield {
      val el1 = el.asInstanceOf[CityBody.Dom_t]
      CityBody(el1)
    }
  }

  /** Инициализация поддержки событий. */
  override def initLayout(fsm: SjsFsm): Unit = {
    onClick { event: Event =>
      val tgVm = Vm(event.target.asInstanceOf[Node])
      for (ngHead <- NgHead.of(tgVm)) {
        fsm !! NgClick(event, ngHead)
      }
    }
  }

}


case class CityBodies(override val _underlying: Dom_t)
  extends CityBodiesT
