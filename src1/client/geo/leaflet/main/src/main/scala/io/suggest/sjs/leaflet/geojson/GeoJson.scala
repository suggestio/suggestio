package io.suggest.sjs.leaflet.geojson

import io.suggest.geo.json.GjFeature
import io.suggest.sjs.leaflet.{PolygonCoords_t, PolygonLatLngs_t}
import io.suggest.sjs.leaflet.map.{LMap, LatLng, LatLngBounds, Layer}
import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 14:44
  * Description: GeoJSON layer model.
  */
@JSImport(LEAFLET_IMPORT, "GeoJSON")
@js.native
object GeoJson extends js.Object {

  def geometryToLayer(feature       : GjFeature,
                      pointToLayer  : js.Function2[GjFeature, LatLng, js.Object] = js.native
                     ): Layer = js.native

  def coordsToLatLng(coords: js.Array[Double]): LatLng = js.native

  def coordsToLatLngs(coords         : PolygonCoords_t,
                      levelsDeep     : Int = js.native, // 0
                      coordsToLatLng : js.Function1[js.Array[Double], LatLng] = js.native // coordsToLatLng()
                     ): PolygonLatLngs_t = js.native

  def latLngToCoords(latLng: LatLng): js.Array[Double] = js.native

  def latLngsToCoords(latLngs     : PolygonLatLngs_t,
                      levelsDeep  : Int     = js.native,
                      closed      : Boolean = js.native
                     ): PolygonCoords_t = js.native

  def asFeature(geojson: js.Object): js.Object = js.native

}


@js.native
sealed trait GeoJson extends Layer {

  def addTo(lmap: LMap): this.type = js.native

  def getBounds(): LatLngBounds = js.native

}
