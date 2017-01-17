package io.suggest.sjs.leaflet.path.poly

import io.suggest.sjs.leaflet.map.LatLngBounds

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 23:08
 * Description: API for rectangles.
 */
@JSImport("leaflet", "Rectangle")
@js.native
class Rectangle extends Polygon {

  def setBounds(bounds: LatLngBounds): this.type = js.native

}
