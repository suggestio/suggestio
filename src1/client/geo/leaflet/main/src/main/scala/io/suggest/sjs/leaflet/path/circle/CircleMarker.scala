package io.suggest.sjs.leaflet.path.circle

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.map.LatLng

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 23:02
 * Description: API for L.CircleMarker.
 * Here radius in pixels, not meters.
 */
@JSImport(LEAFLET_IMPORT, "circleMarker")
@js.native
object CircleMarker extends js.Function2[LatLng, CircleMarkerOptions, Circle] {
  override def apply(arg1: LatLng, arg2: CircleMarkerOptions = js.native): Circle = js.native
}


@JSImport(LEAFLET_IMPORT, "CircleMarker")
@js.native
class CircleMarker(latLng   : LatLng,
                   options  : CircleMarkerOptions = js.native) extends Circle
