package cordova.plugins.diagnostic

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 17:48
  */
@js.native
trait BluetoothStates extends js.Object {

  val UNKNOWN, POWERED_OFF, POWERED_ON: BluetoothState_t = js.native

  // Android
  val POWERING_OFF, POWERING_ON: js.UndefOr[BluetoothState_t] = js.native

  // iOS
  val RESETTING, UNAUTHORIZED: js.UndefOr[BluetoothState_t] = js.native

}
