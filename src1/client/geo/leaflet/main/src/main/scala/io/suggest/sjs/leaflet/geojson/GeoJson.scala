package io.suggest.sjs.leaflet.geojson

import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.leaflet.map.{LMap, LatLng, LatLngBounds, Layer}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 14:44
  * Description: GeoJSON layer model.
  */
@JSImport("leaflet", "GeoJSON")
@js.native
object GeoJson extends js.Object {

  def geometryToLayer(feature       : GjFeature,
                      pointToLayer  : js.Function2[GjFeature, LatLng, js.Object] = js.native
                     ): Layer = js.native

  def coordsToLatLng(coords: js.Array[Double]): LatLng = js.native

  def coordsToLatLng(coords         : js.Object,
                     levelsDeep     : Int = js.native,
                     coordsToLatLng : js.Function1[js.Array[Double], LatLng] = js.native
                    ): LatLng = js.native

  def latLngToCoords(latLng: LatLng): js.Array[Double] = js.native

  def latLngsToCoords(latLngs     : js.Array[js.Any],
                      levelsDeep  : Int     = js.native,
                      closed      : Boolean = js.native
                     ): js.Array[js.Any] = js.native

  def asFeature(geojson: js.Object): js.Object = js.native

}


@js.native
sealed trait GeoJson extends Layer {

  def addTo(lmap: LMap): this.type = js.native

  def getBounds(): LatLngBounds = js.native

}
