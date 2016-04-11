package io.suggest.sjs.leaflet.geojson

import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.leaflet.map.{ILayer, LatLng}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 14:07
  * Description: GeoJSON layers builder constructor options.
  *
  * @see [[http://leafletjs.com/reference.html#geojson]]
  * @see [[http://leafletjs.com/examples/geojson.html]]
  */
object GjOptions extends FromDict {
  override type T = GjOptions
}


@js.native
sealed trait GjOptions extends js.Object {

  def style(feature: GjFeature): GjFeatureStyle = js.native

  def pointToLayer(feature: GjFeature, latLng: LatLng): js.Any = js.native

  def onEachFeature(feature: GjFeature, layer: ILayer): Unit = js.native

  def filter(feature: GjFeature, layer: ILayer): Boolean = js.native

  def coordsToLatLng(coords: js.Array[Double]): LatLng = js.native

}
