package io.suggest.sjs.leaflet.map

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 17:03
  * Description: API for LatLngBounds model.
  */
@JSImport("leaflet", "LatLngBounds")
@js.native
class LatLngBounds extends js.Object {

  def extend(latLng: LatLng | LatLngBounds): this.type = js.native

  def getSouthWest(): LatLng = js.native

  def getNorthEast(): LatLng = js.native

  def getNorthWest(): LatLng = js.native

  def getSouthEast(): LatLng = js.native

  def getWest(): Double = js.native

  def getSouth(): Double = js.native

  def getEast(): Double = js.native

  def getNorth(): Double = js.native

  def getCenter(): LatLng = js.native

  def contains(other: LatLngBounds): Boolean = js.native

  def contains(latLng: LatLng): Boolean = js.native

  def intersects(bounds: LatLngBounds): Boolean = js.native

  def equals(bounds: LatLngBounds): Boolean = js.native

  def toBBoxString(): String = js.native

  def pad(bufferRatio: Double): LatLngBounds = js.native

  def isValid(): Boolean = js.native

}
