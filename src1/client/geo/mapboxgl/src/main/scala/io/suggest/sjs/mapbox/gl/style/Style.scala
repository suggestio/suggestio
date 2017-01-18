package io.suggest.sjs.mapbox.gl.style

import io.suggest.sjs.mapbox.gl.layer.Layer
import io.suggest.sjs.mapbox.gl.source.SourceDescr
import io.suggest.sjs.mapbox.gl.{Bearing_t, Pitch_t, Zoom_t}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 14:00
  * Description: MapBox style model API.
  *
  * @see [[https://www.mapbox.com/mapbox-gl-style-spec]]
  */

//@JSName("mapboxgl.Style")
@js.native
trait Style extends js.Object {

  val version: Int = js.native

  val name: UndefOr[String] = js.native

  val center: UndefOr[js.Array[Double]] = js.native

  val zoom: UndefOr[Zoom_t] = js.native

  val bearing: UndefOr[Bearing_t] = js.native

  val pitch: UndefOr[Pitch_t] = js.native

  val sources: js.Dictionary[SourceDescr] = js.native

  val sprite: UndefOr[String] = js.native

  val glyphs: UndefOr[String] = js.native

  val transition: UndefOr[Transition] = js.native

  val layers: UndefOr[js.Array[Layer]] = js.native

}
