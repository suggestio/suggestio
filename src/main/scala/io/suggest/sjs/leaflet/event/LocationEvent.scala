package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.map.{LatLng, LatLngBounds}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:54
 * Description: API for leaflet location event.
 */

@js.native
class LocationEvent extends Event {

  @JSName("latlng")
  var latLng: LatLng = js.native

  var bounds: LatLngBounds = js.native

  var accuracy: Double = js.native

  var altitude: Double = js.native

  var altitudeAccuracy: Double = js.native

  var heading: Double = js.native

  var speed: Double = js.native

  var timestamp: Double = js.native

}
