package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.10.2020 17:28
  * @see https://github.com/ebhsgit/cordova-plugin-background-geolocation#location-event
  */
@js.native
trait Location extends js.Object {

  /** ID of location as stored in DB (or null).
    * Do not use location id as unique key in your database as ids will be reused when option.maxLocations is reached.
    */
  val id: js.UndefOr[LocationId_t]

  /** @see [[Location.Provider]]. */
  val provider: String

  val locationProvider: LocationProvider_t

  /** UTC time of this fix, in milliseconds since January 1, 1970. */
  val time: Double

  val latitude, longitude: Double

  /** Estimated accuracy of this location, in meters. */
  val accuracy: js.UndefOr[Double]

  /** Speed if it is available, in meters/second over ground. */
  val speed: js.UndefOr[Double]

  /** Altitude if available, in meters above the WGS 84 reference ellipsoid. */
  val altitude: js.UndefOr[Double]

  /** Bearing, in degrees. */
  val bearing: js.UndefOr[Double]

  // Locations parameters isFromMockProvider and mockLocationsEnabled are not posted to url or syncUrl by default.
  // Both can be requested via option postTemplate.

  /** (android only) True if location was recorded by mock provider. */
  val isFromMockProvider: js.UndefOr[Boolean]

  /** (android only) True if device has mock locations enabled. */
  val mockLocationsEnabled: js.UndefOr[Boolean]

}

object Location {

  object Provider {
    final def gps = "gps"
    final def network = "network"
    final def passive = "passive"
    final def fused = "fused"
  }

}
