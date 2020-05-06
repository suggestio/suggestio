package io.suggest.sw

import org.scalajs.dom.experimental.serviceworkers.{ServiceWorker, ServiceWorkerMessageEventInit}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.18 21:43
  * Description: Утиль для ServiceWorker'ов.
  */
object ServiceWorkerUtil {

  object Implicits {

    implicit class SwMessageEventOpsExt( val event: ServiceWorkerMessageEventInit ) extends AnyVal {

      /** Почему-то нет прямого общего API для вызова source.postMessage() . */
      def sourceSw: ServiceWorker =
        event.source.asInstanceOf[ServiceWorker]

    }

  }

}



/** Модель опций ServiceWorker'а. */
trait SwOptions extends js.Object {

  val scope: js.UndefOr[String] = js.undefined

}
