package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 18:44
  * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation/blob/master/www/BackgroundGeolocation.d.ts#L373]]
  */
@js.native
trait StationaryLocation extends js.Object {

  val radius: Double = js.native

}
