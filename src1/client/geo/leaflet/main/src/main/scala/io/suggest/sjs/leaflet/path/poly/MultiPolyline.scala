package io.suggest.sjs.leaflet.path.poly

import io.suggest.sjs.leaflet.map.LatLng
import io.suggest.sjs.leaflet.path.Path

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 11:22
 * Description: Extends FeatureGroup to allow creating multi-polylines
 * (single layer that consists of several polylines that share styling/popup).
 */
@js.native
@JSName("L.MultiPolyline")
class MultiPolyline extends Path {

  def setLatLngs(latLngs: js.Array[js.Array[LatLng]]): this.type = js.native

  def getLatLngs(): js.Array[js.Array[LatLng]] = js.native

  def openPopup(): this.type = js.native

  def toGeoJSON(): js.Object = js.native

}
