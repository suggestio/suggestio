package io.suggest.sjs.mapbox.gl.style

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.layer.Layer
import io.suggest.sjs.mapbox.gl.source.SourceDescr
import io.suggest.sjs.mapbox.gl.{Bearing_t, Pitch_t, Zoom_t}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:00
  * Description: MapBox style model API.
  *
  * @see [[https://www.mapbox.com/mapbox-gl-style-spec]]
  */
object Style extends FromDict {
  override type T = Style
}

@js.native
@JSName("mapboxgl.Style")
trait Style extends js.Object {

  var version: Int = js.native

  var name: UndefOr[String] = js.native

  var center: UndefOr[js.Array[Double]] = js.native

  var zoom: UndefOr[Zoom_t] = js.native

  var bearing: UndefOr[Bearing_t] = js.native

  var pitch: UndefOr[Pitch_t] = js.native

  var sources: js.Dictionary[SourceDescr] = js.native

  var sprite: UndefOr[String] = js.native

  var glyphs: UndefOr[String] = js.native

  var transition: UndefOr[Transition] = js.native

  var layers: UndefOr[js.Array[Layer]] = js.native

}
