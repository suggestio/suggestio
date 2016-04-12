package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.{Bearing_t, Zoom_t}
import io.suggest.sjs.mapbox.gl.ll.LngLatBounds
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:15
  * Description: API for map constructor options JSON.
  * @see [[https://www.mapbox.com/mapbox-gl-js/api/#Map]]
  */
object GlMapOptions extends FromDict {
  override type T = GlMapOptions
}


@js.native
sealed trait GlMapOptions extends SupportedOptions {

  var touchZoomRotate: Boolean = js.native

  var minZoom: Zoom_t = js.native
  var maxZoom: Zoom_t = js.native

  var style: js.Object | String = js.native

  var hash: Boolean = js.native

  var interactive: Boolean = js.native

  var bearingSnap: Bearing_t = js.native

  var classes: js.Array[String] = js.native

  var attributionControl: Boolean = js.native

  var container: String | Element = js.native

  var preserveDrawingBuffer: Boolean = js.native

  var maxBounds: LngLatBounds | js.Array[js.Array[Double]] = js.native

  var scrollZoom: Boolean = js.native

  var boxZoom: Boolean = js.native

  var dragRotate: Boolean = js.native

  var dragPan: Boolean = js.native

  var keyboard: Boolean = js.native

  var doubleClickZoom: Boolean = js.native

}
