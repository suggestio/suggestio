package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 18:54
  * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation/blob/master/www/BackgroundGeolocation.d.ts#L429]]
  */
@js.native
trait LogEntry extends js.Object {

  val id: Int = js.native

  val timestamp: Int = js.native

  val level: LogLevel = js.native

  val message: String = js.native

  val stackTrace: String = js.native

}
