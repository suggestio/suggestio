package io.suggest.sjs

import io.suggest.err.ToThrowable
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.2020 0:00
  * Description: Утиль для облегчения взаимодействия с обычным js-api.
  */
object JsApiUtil extends Log {

  /** Безопасный синхронный вызов дял асинхрона. */
  private def apiCallSyncSafe[T](p: Promise[T])
                                (f: => Unit): Future[T] = {
    Try( f )
      .failed
      .foreach { ex =>
        logger.error( ErrorMsgs.NATIVE_API_ERROR, ex, this )
        p.failure(ex)
      }
    p.future
  }

  def call0Fut(f: js.Function0[Unit] => Unit): Future[Unit] = {
    val p = Promise[Unit]()
    apiCallSyncSafe(p)( f(() => p.success()) )
  }

  def call1Fut[T1](f: js.Function1[T1, Unit] => Unit): Future[T1] = {
    val p = Promise[T1]()
    apiCallSyncSafe(p)( f(p.success) )
  }

  def call0ErrFut[E: ToThrowable](f: (js.Function0[Unit], js.Function1[E, Unit]) => Unit): Future[Unit] = {
    val p = Promise[Unit]()
    apiCallSyncSafe(p)( f(() => p.success(()), p.failure(_)) )
  }

  def call1ErrFut[T, E: ToThrowable](f: (js.Function1[T, Unit], js.Function1[E, Unit]) => Unit): Future[T] = {
    val p = Promise[T]()
    apiCallSyncSafe(p)( f(p.success, p.failure(_)) )
  }


  /** js.isUndefined() начиная с scalajs-1.0 ведёт себя небезопасно по отношению к native-классам и объектам,
    * возвращая экзепшен вместо false.
    * Тут безопасная обёртка над этим делом.
    *
    * @param und То, что тестируем на безопасность (функция).
    * @return true, если указанная сущность имеет место быть.
    *         false, если не удаётся подтвердить существенность указанного кода.
    */
  def isDefinedSafe( und: => js.Any ): Boolean = {
    Try( !js.isUndefined(und) )
      .getOrElse( false )
  }

}
