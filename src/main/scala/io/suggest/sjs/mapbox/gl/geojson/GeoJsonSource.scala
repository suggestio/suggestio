package io.suggest.sjs.mapbox.gl.geojson

import io.suggest.sjs.common.geo.json.GjType

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 22:17
  * Description: GeoJSON source APIs.
  */

@js.native
@JSName("mapboxgl.GeoJSONSource")
class GeoJsonSource(options: GeoJsonSourceDescr) extends js.Object {

  def setData(data: GjType): this.type = js.native

}



