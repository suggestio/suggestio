package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 18:40
  * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation/blob/master/www/BackgroundGeolocation.d.ts#L392]]
  */
@js.native
trait BackgroundGeolocationError extends js.Object {

  def code: Int = js.native

  def message: String = js.native

}
