package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 19:14
  * Description: Options for calling geolocation API.
  */
@js.native
sealed trait LocateOptions extends js.Object {

  var watch: Boolean = js.native
  var setView: Boolean = js.native
  var maxZoom: Double = js.native
  var timeout: Double = js.native
  var maximumAge: Double = js.native
  var enableHighAccuracy: Boolean = js.native

}
