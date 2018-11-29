package io.suggest.sc.sw.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.sw.m.{HandleFetch, HandleMessage, MScSwRoot}
import io.suggest.spa.ServiceWorkerUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.experimental.{Fetch, Response}
import org.scalajs.dom.experimental.serviceworkers.FetchEvent
import org.scalajs.dom.experimental.serviceworkers.ServiceWorkerGlobalScope.self

import scala.scalajs.js.JSConverters._
import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:35
  * Description: Корневой контроллер выдачи.
  */
object ScSwRootAh {

  val CACHE_NAME = "v1"

}

class ScSwRootAh[M](
                     modelRW: ModelRW[M, MScSwRoot]
                   )
  extends ActionHandler(modelRW)
{

  import ScSwRootAh._

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Проксирование запросов выдачи.
    case m: HandleFetch =>
      // Сюда приходят только запросы к API.
      // API-запросы - кэшируем, но отвечаем с приоритетом запроса.
      val jsRespFut = self.caches
        .`match`(m.event.request)
        .toFuture
        .asInstanceOf[Future[js.UndefOr[Response]]]
        .flatMap { responseUndef =>
          responseUndef.fold {
            Fetch.fetch( m.event.request )
              .toFuture
          } { response =>
            // Сохранить ответ в кэш.
            val cacheStorageFut = self.caches
              .open(CACHE_NAME)
              .toFuture
            val responseCloned = response.clone()
            for (cache <- cacheStorageFut)
              cache.put( m.event.request, responseCloned )

            Future.successful( response )
          }
        }
        .toJSPromise
      m.event.respondWith( jsRespFut )

      noChange


    // Сообщение через postMessage() из выдачи.
    case m: HandleMessage =>
      println(m)

      // Ответить назад.
      val reply = "pong"

      m.event.sourceSw.postMessage( reply )

      noChange

  }

}
