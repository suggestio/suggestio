package io.suggest.sjs.mapbox.gl.ll

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:26
  * Description: API for LngLat instances.
  *
  * @see [[https://www.mapbox.com/mapbox-gl-js/api/#LngLat]]
  */
@js.native
@JSGlobal("mapboxgl.LngLat")
object LngLat extends js.Object {

  def convert(input: js.Array[Double] | LngLat): LngLat = js.native

}


@js.native
@JSGlobal("mapboxgl.LngLat")
class LngLat(val lng: Double, val lat: Double) extends js.Object {

  def toArray(): js.Array[Double] = js.native

  def wrap(): LngLat = js.native

}
