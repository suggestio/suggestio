package io.suggest.sjs.leaflet.geojson

import io.suggest.sjs.common.geo.json.GjFeature
import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.leaflet.map.{LatLng, Layer}
import io.suggest.sjs.leaflet.path.PathOptions

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.16 14:07
  * Description: GeoJSON layers builder constructor options.
  *
  * @see [[http://leafletjs.com/reference.html#geojson]]
  * @see [[http://leafletjs.com/examples/geojson.html]]
  *
  * Need scala 0.6.14+.
  */
object GjOptions extends FromDict {

  override type T = GjOptions

}


@ScalaJSDefined
trait GjOptions extends js.Object {

  val style: js.UndefOr[js.Function1[GjFeature, PathOptions]]          = js.undefined

  val pointToLayer: js.UndefOr[js.Function2[GjFeature, LatLng, Layer]]    = js.undefined

  val onEachFeature: js.UndefOr[js.Function2[GjFeature, Layer, Unit]]     = js.undefined

  val filter: js.UndefOr[js.Function2[GjFeature, Layer, Boolean]]         = js.undefined

  val coordsToLatLng: js.UndefOr[js.Function1[js.Array[Double], LatLng]]  = js.undefined

}
