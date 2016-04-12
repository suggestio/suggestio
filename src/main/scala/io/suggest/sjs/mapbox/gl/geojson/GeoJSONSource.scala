package io.suggest.sjs.mapbox.gl.geojson

import io.suggest.sjs.mapbox.gl.style.source.Source

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:49
  * Description:
  */
@js.native
@JSName("mapboxgl.GeoJSONSource")
class GeoJSONSource(options: GjOptions) extends Source {

  def setData(data: js.Object | String): this.type = js.native

}
