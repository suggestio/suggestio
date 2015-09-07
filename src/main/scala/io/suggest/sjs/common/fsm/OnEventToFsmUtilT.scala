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
trait OnEventToFsmUtilT extends EventTargetVmT with SendEventToFsmUtil {

  /**
   * Собрать и повесить листенер проброски события в FSM на указанное событие указанного типа.
   * @param eventType DOM-тип события.
   * @param model Сборщик экземпляров сигналов.
   * @tparam Event_t Класс DOM-события.
   */
  protected def _addToFsmEventListener[Event_t <: Event](eventType: String, model: IFsmMsgCompanion[Event_t]): Unit = {
    val f = _sendEventF[Event_t](model)
    addEventListener(eventType)(f)
  }

}
