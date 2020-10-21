package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 18:05
  * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation/blob/master/www/BackgroundGeolocation.d.ts#L548]]
  */
@js.native
trait ServiceStatus extends js.Object {

  def isRunning: Boolean = js.native

  def locationServicesEnabled: Boolean = js.native

  def authorization: AuthorizationStatus = js.native

}
