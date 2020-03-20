package io.suggest.sjs

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.2020 0:00
  * Description: Утиль для облегчения взаимодействия с обычным js-api.
  */
object JsApiUtil {

  def call0Fut(f: js.Function0[Unit] => Unit): Future[Unit] = {
    val p = Promise[Unit]()
    f( () => p.success() )
    p.future
  }

  def call1Fut[T1](f: js.Function1[T1, Unit] => Unit): Future[T1] = {
    val p = Promise[T1]()
    f( p.success )
    p.future
  }

}
