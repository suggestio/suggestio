package io.suggest.sc.sw

import io.suggest.common.event.DomEvents
import io.suggest.sc.sw.m.{HandleFetch, HandleMessage}
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import org.scalajs.dom.experimental.serviceworkers.ServiceWorkerGlobalScope.self
import org.scalajs.dom.experimental.serviceworkers.{FetchEvent, ServiceWorkerMessageEvent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:18
  * Description: Запуск ServiceWorker'а выдачи начинается отсюда.
  */
object ScSwMain {

  def main(args: Array[String]): Unit = {
    val modules = new ScSwModule
    val circuit = modules.circuit

    // Подписка на события ServiceWorker'а:
    self.addEventListener4s( DomEvents.MESSAGE ) { e: ServiceWorkerMessageEvent =>
      circuit.dispatch( HandleMessage(e) )
    }

    self.addEventListener4s( DomEvents.FETCH ) { e: FetchEvent =>
      circuit.dispatch( HandleFetch(e) )
    }

    // TODO onactivate, oninstall: ExtendableEvent

    // Где-то писали, что надо после инициализации postMessage() вызывать без аргументов. Или не надо?
  }

}
