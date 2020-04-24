package io.suggest.sjs

import io.suggest.err.ToThrowable

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

  def call0ErrFut[E: ToThrowable](f: (js.Function0[Unit], js.Function1[E, Unit]) => Unit): Future[Unit] = {
    val p = Promise[Unit]()
    f( () => p.success(()), p.failure(_) )
    p.future
  }

  def call1ErrFut[T, E: ToThrowable](f: (js.Function1[T, Unit], js.Function1[E, Unit]) => Unit): Future[T] = {
    val p = Promise[T]()
    f( p.success, p.failure(_) )
    p.future
  }

}
