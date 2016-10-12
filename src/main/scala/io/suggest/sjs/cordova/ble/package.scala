package io.suggest.sjs.cordova

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 14:38
  */
package object ble {

  /**
    * This function is called when an operation fails.
    * $1 - A human-readable string that describes the error that occurred.
    */
  type ErrorCallback_t = js.Function1[String, _]

  type DevHandle_t     = Int

  type ServiceHandle_t = Int

  type CharacteristicHandle_t = Int

  /** A negative integer, the signal strength in decibels. */
  type Rssi_t          = Int

}
