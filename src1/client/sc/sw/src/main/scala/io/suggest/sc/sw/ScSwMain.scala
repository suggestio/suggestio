package io.suggest.sc.sw

import com.github.gcl.swtoolbox.SwToolBox
import io.suggest.common.event.DomEvents
import io.suggest.sc.sw.m.{HandleFetch, HandleMessage}
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import org.scalajs.dom.experimental.serviceworkers.ServiceWorkerGlobalScope.self
import org.scalajs.dom.experimental.serviceworkers.{ExtendableEvent, FetchEvent, ServiceWorkerMessageEvent}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:18
  * Description: Запуск ServiceWorker'а выдачи начинается отсюда.
  */
object ScSwMain {

  def main(args: Array[String]): Unit = {

    self.addEventListener4s( DomEvents.ACTIVATE ) { e: ExtendableEvent =>
      e.waitUntil(
        self.caches
          .keys()
          .toFuture
          .flatMap { keysArr =>
            Future.traverse(keysArr.toIterable) { k =>
              self.caches
                .delete(k)
                .toFuture
            }
          }
          .toJSPromise
      )
    }

    // Обновлять корневую страницу в фоне.
    SwToolBox.router.any("/", SwToolBox.fastest)

    // API-вызовы кэшировать, но делать.
    // TODO Разобраться, что и как кэшировать и использовать.
    SwToolBox.router.get("/sc/.+", SwToolBox.networkFirst)

    //SwToolBox.router.post("/sc/.+", SwToolBox.networkOnly)
    SwToolBox.router.any("/.+", SwToolBox.networkOnly)
  }


  private def main0(args: Array[String]): Unit = {
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
