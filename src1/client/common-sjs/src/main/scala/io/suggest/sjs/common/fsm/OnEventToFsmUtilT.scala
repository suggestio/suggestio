package io.suggest.sjs.common.fsm

import io.suggest.sjs.common.vm.evtg.EventTargetVmT
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 17:07
 * Description: Трейт для добавления поддержки в VM метода вешанья листенеров, пробрасывающих завёрнутые
 * события в какой-то FSM.
 */

/** Бывает, что fsm передаётся в init как аргумент. */
trait OnEventToArgFsmUtilT extends EventTargetVmT {

  protected def _addToFsmEventListener[Event_t <: Event](fsm: SjsFsm, eventType: String, model: IFsmMsgCompanion[Event_t]): Unit = {
    val f = SendEventToFsmUtil.f[Event_t](fsm, model)
    addEventListener(eventType)(f)
  }
}
