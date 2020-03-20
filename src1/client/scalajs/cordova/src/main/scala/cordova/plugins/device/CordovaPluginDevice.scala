package cordova.plugins.device

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.2020 23:25
  * Description: API for cordova-plugin-device.
  * @see [[https://github.com/apache/cordova-plugin-device#properties]]
  */
@js.native
trait CordovaPluginDevice extends js.Object {

  val cordova: String = js.native
  val model: String = js.native
  val platform: String = js.native
  val uuid: String = js.native
  val version: String = js.native
  val manufacturer: String = js.native
  val isVirtual: String = js.native
  val serial: String = js.native

}
