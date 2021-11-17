package io.suggest.sc.sw

import com.github.gcl.swtoolbox.SwToolBox
import io.suggest.event.DomEvents
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import org.scalajs.dom.experimental.serviceworkers.ServiceWorkerGlobalScope.self
import org.scalajs.dom.experimental.serviceworkers.ExtendableEvent
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
      val caches = self.caches
      e.waitUntil(
        caches
          .keys()
          .toFuture
          .flatMap { keysArr =>
            Future.traverse(keysArr.toIterable) { k =>
              caches
                .delete(k)
                .toFuture
            }
          }
          .toJSPromise
      )
    }

    // Обновлять корневую страницу в фоне.
    SwToolBox.router.any("/", SwToolBox.networkFirst)

    // API-вызовы кэшировать, но делать.
    // TODO Разобраться, что и как кэшировать и использовать.
    SwToolBox.router.get("/sc/.+", SwToolBox.networkFirst)

    //SwToolBox.router.post("/sc/.+", SwToolBox.networkOnly)
    SwToolBox.router.any("/.+", SwToolBox.networkOnly)
  }

}
