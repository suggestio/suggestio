package io.suggest.sjs.common.model

import scala.concurrent.Promise

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.16 16:08
  * Description: Контейнер для promise'а, который должен исполнится по таймауту.
  */
case class TimeoutPromise[T](
  promise : Promise[T],
  timerId : Int
) {

  lazy val fut = promise.future

}
