package cordova.plugins.diagnostic

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 17:29
  */
@js.native
trait LocationModes extends js.Object {

  val HIGH_ACCURACY, BATTERY_SAVING, DEVICE_ONLY, LOCATION_OFF: LocationMode_t = js.native

}
