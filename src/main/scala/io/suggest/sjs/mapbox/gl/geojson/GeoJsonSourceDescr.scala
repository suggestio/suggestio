package io.suggest.sjs.mapbox.gl.geojson

import io.suggest.sjs.common.geo.json.GjType
import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Zoom_t
import io.suggest.sjs.mapbox.gl.source.{SourceDescr, SourceTypes}

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.04.16 22:22
  * Description: GeoJSON Source descriptor.
  */
object GeoJsonSourceDescr extends FromDict {

  override type T = GeoJsonSourceDescr

  def apply(data: GjType | String): GeoJsonSourceDescr = {
    val gjsd = empty
    gjsd.`type` = SourceTypes.GEOJSON
    gjsd.data = data
    gjsd
  }

}


@js.native
class GeoJsonSourceDescr extends SourceDescr {

  var data: GjType | String = js.native

  var buffer: Int = js.native

  var tolerance: Int = js.native

  var cluster: Boolean = js.native

  var clusterRadius: Int = js.native

  var clusterMaxZoom: Zoom_t = js.native

}
