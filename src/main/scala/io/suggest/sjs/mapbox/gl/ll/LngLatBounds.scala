package io.suggest.sjs.mapbox.gl.ll

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:31
  * Description: API for LatLngBounds.
  */

@js.native
@JSName("mapboxgl.LngLatBounds")
object LngLatBounds extends js.Object {

  def convert(input: LngLatBounds | js.Array[Double] | js.Array[js.Array[Double]]): LngLatBounds = js.native

}


@js.native
@JSName("mapboxgl.LngLatBounds")
class LngLatBounds(sw: LngLat, nw: LngLat) extends js.Object {

  def extend(obj: LngLat | LngLatBounds): this.type = js.native

  def getCenter(): LngLat     = js.native

  def getEast(): Double       = js.native
  def getNorth(): Double      = js.native
  def getNorthEast(): LngLat  = js.native
  def getNorthWest(): LngLat  = js.native
  def getSouth(): Double      = js.native
  def getSouthEast(): LngLat  = js.native
  def getSouthWest(): LngLat  = js.native
  def getWest(): Double       = js.native

  def toArray(): js.Array[js.Array[Double]] = js.native



}
