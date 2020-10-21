package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 17:20
  * @see [[https://github.com/ebhsgit/cordova-plugin-background-geolocation/blob/master/www/BackgroundGeolocation.d.ts#L298]]
  */
trait LocationOptions extends js.Object {

  /** Maximum time in milliseconds device will wait for location. */
  val timeout: js.UndefOr[Double] = js.undefined

  /** Maximum age in milliseconds of a possible cached location that is acceptable to return. */
  val maximumAge: js.UndefOr[Double] = js.undefined

  /** If true and if the device is able to provide a more accurate position, it will do so */
  val enableHighAccuracy: js.UndefOr[Boolean] = js.undefined

}
