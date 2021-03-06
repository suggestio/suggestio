package io.suggest.sjs.leaflet.path.poly

import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.path.Path

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 23:10
 * Description: API for polylines.
 */
@JSImport(LEAFLET_IMPORT, "Polyline")
@js.native
class Polyline extends Path {

  def addLatLng(latLng: LatLng): this.type = js.native

  def setLatLngs(latLngs: js.Array[LatLng]): this.type = js.native

  def getLatLngs(): js.Array[LatLng] = js.native

  def spliceLatLngs(index: Int, pointsToRemove: Int, latLng: LatLng*): js.Array[LatLng] = js.native

  def toGeoJSON(): js.Object = js.native

}
