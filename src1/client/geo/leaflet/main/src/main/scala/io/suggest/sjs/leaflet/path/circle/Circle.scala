package io.suggest.sjs.leaflet.path.circle

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.path.Path

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:22
 * Description: API for circles.
 */
@JSImport(LEAFLET_IMPORT, "Circle")
@js.native
class Circle extends Path {

  def getLatLng(): LatLng = js.native

  def getRadius(): Double = js.native

  def setLatLng(latLng: LatLng): this.type = js.native

  def setRadius(radiusM: Double): this.type = js.native

  def toGeoJSON(): js.Object = js.native

}
