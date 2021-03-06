package cordova.plugins.ble.central

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.06.18 15:37
  */
package object central {

  type Services_t = js.Array[String]

  /** A negative integer, the signal strength in decibels. */
  type Rssi_t = Int

}
