package io.suggest.spa

import org.scalajs.dom.experimental.serviceworkers.{ServiceWorker, ServiceWorkerMessageEvent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:43
  * Description: Утиль для ServiceWorker'ов.
  */
object ServiceWorkerUtil {

  object Implicits {

    implicit class SwMessageEventOpsExt( val event: ServiceWorkerMessageEvent ) extends AnyVal {

      /** Почему-то нет прямого общего API для вызова source.postMessage() . */
      def sourceSw: ServiceWorker =
        event.source.asInstanceOf[ServiceWorker]

    }

  }

}
