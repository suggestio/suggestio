package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 17:54
  * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation#getcurrentlocationsuccess-fail-options]]
  */
@js.native
trait LocationError extends js.Object {

  def code: LocationErrorCode = js.native

  def message: String = js.native

}
