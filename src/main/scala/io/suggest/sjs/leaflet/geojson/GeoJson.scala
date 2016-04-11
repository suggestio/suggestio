package io.suggest.sjs.leaflet.geojson

import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.leaflet.map.{ILayer, LMap, LatLng, LatLngBounds}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 14:44
  * Description: GeoJSON layer model.
  */
@js.native
@JSName("L.GeoJSON")
object GeoJson extends js.Object {

  def geometryToLayer(feature: GjFeature,
                      pointToLayer: js.Function2[GjFeature, LatLng, js.Object] = js.native): ILayer = js.native

  def coordsToLatLng(coords: js.Array[Double], reverse: Boolean = js.native): LatLng = js.native

  def coordsToLatLngs(coords: GjCoordsDeepArray,
                      levelsDeep: Int = js.native,
                      reverse: Boolean = js.native): GjLatLngDeepArray = js.native

}


@js.native
sealed class GeoJson extends ILayer {

  def addTo(lmap: LMap): this.type = js.native

  def getBounds(): LatLngBounds = js.native

}
