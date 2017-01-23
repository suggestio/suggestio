package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.mapbox.gl.{Bearing_t, Zoom_t}
import io.suggest.sjs.mapbox.gl.ll.LngLatBounds
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined
import scala.scalajs.js.{UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:15
  * Description: API for map constructor options JSON.
  * @see [[https://www.mapbox.com/mapbox-gl-js/api/#Map]]
  */

@ScalaJSDefined
trait GlMapOptions extends SupportedOptions {

  val touchZoomRotate: UndefOr[Boolean] = js.undefined

  val minZoom: UndefOr[Zoom_t] = js.undefined
  val maxZoom: UndefOr[Zoom_t] = js.undefined

  val style: js.UndefOr[js.Object | String] = js.undefined

  val hash: js.UndefOr[Boolean] = js.undefined

  val interactive: js.UndefOr[Boolean] = js.undefined

  val bearingSnap: js.UndefOr[Bearing_t] = js.undefined

  val classes: js.UndefOr[js.Array[String]] = js.undefined

  val attributionControl: js.UndefOr[Boolean] = js.undefined

  val container: js.UndefOr[String | Element] = js.undefined

  val preserveDrawingBuffer: js.UndefOr[Boolean] = js.undefined

  val maxBounds: js.UndefOr[LngLatBounds | js.Array[js.Array[Double]]] = js.undefined

  val scrollZoom: js.UndefOr[Boolean] = js.undefined

  val boxZoom: js.UndefOr[Boolean] = js.undefined

  val dragRotate: js.UndefOr[Boolean] = js.undefined

  val dragPan: js.UndefOr[Boolean] = js.undefined

  val keyboard: js.UndefOr[Boolean] = js.undefined

  val doubleClickZoom: js.UndefOr[Boolean] = js.undefined

  var center: js.UndefOr[js.Array[Double]] = js.undefined

  var zoom: js.UndefOr[Zoom_t] = js.undefined

}
