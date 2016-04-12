package io.suggest.sjs.mapbox.gl.geojson

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Zoom_t

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:50
  * Description: GeoJSON constructor options.
  */
object GjOptions extends FromDict {
  override type T = GjOptions
}


@js.native
trait GjOptions extends js.Object {

  /** GeoJSON objects or URL string. */
  var data: js.Object | String = js.native

  var maxzoom: Zoom_t = js.native

  var buffer: Int = js.native

  var tolerance: Int = js.native

  var cluster: Boolean = js.native

  var clusterRadius: Int = js.native

  var clusterMaxZoom: Zoom_t = js.native

}
