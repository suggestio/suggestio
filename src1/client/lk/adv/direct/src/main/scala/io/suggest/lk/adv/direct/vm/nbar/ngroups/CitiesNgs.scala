package io.suggest.lk.adv.direct.vm.nbar.ngroups

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.lk.adv.direct.fsm.AdvDirectFormFsm
import io.suggest.lk.adv.direct.m.NodeChecked
import io.suggest.lk.adv.direct.vm.nbar.nodes.NodeCheckBox
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.vm.evtg.OnMouseClickT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.of.{ChildrenVms, OfDiv}
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 19:36
 * Description: Контейнер всех [[CityNgs]] во всех городах.
 */
object CitiesNgs extends FindDiv with OfDiv {
  override type T     = CitiesNgs
  override def DOM_ID = AdvDirectFormConstants.NGRPS_CONT_ID

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      el.id == DOM_ID
    }
  }
}


import CitiesNgs.Dom_t


trait CitiesNgsT extends OnMouseClickT with IInitLayoutFsm with ChildrenVms {

  override type T = Dom_t
  override type ChildVm_t = CityNgs
  override protected def _childVmStatic = CityNgs

  def cities = _childrenVms

  override def initLayout(fsm: SjsFsm): Unit = {
    addEventListener("change") { event: Event =>
      // Нужно, чтобы все чекбоксы внутри контейнера выставляли себе value="BOOL" при смене isChecked
      // TODO value наверное не надо обновлять, если сделать form mapping на сервере более гибким.
      for (cb <- NodeCheckBox.ofEventTarget(event.target)) {
        cb._underlying.value = cb.isChecked.toString
        // Нужно уведомлять FSM о произошедшем
        for (ncb <- NodeCheckBox.ofEl(cb._underlying)) {
          AdvDirectFormFsm !! NodeChecked(ncb, event)
        }
      }
    }
  }

}


case class CitiesNgs(override val _underlying: Dom_t)
  extends CitiesNgsT
