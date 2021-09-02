package io.suggest.async

import diode.data.Pot

import scala.concurrent.Promise


/** Typeclass interface for checking/monitoring of value readyness,
  * and then complete the promise when some value is ready/failed/etc. */
sealed trait IValueCompleter[T] {
  /** Test value for readyness. If value is ready/failed, then complete the promise. */
  def maybeCompletePromise( readyPromise: Promise[None.type], value: T ): Unit
}

object IValueCompleter {

  /** Complete promise, when boolean value becoming true. */
  implicit def booleanValueSubscriber: IValueCompleter[Boolean] = {
    new IValueCompleter[Boolean] {
      override def maybeCompletePromise(readyPromise: Promise[None.type], value: Boolean): Unit =
        if (value)
          readyPromise.trySuccess( None )
    }
  }

  /** Complete promise, when pot value is ready/failed/unavailable. */
  implicit def PotSubscriber[T]: IValueCompleter[Pot[T]] = {
    new IValueCompleter[Pot[T]] {
      override def maybeCompletePromise(readyPromise: Promise[None.type], pot: Pot[T]): Unit = {
        if (!pot.isPending) {
          if (pot.isReady || pot.isUnavailable)
            readyPromise.trySuccess( None )
          else for (ex <- pot.exceptionOption)
            readyPromise.tryFailure( ex )
        }
      }
    }
  }

}
