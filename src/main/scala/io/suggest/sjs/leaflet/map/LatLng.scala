package io.suggest.sjs.leaflet.map

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 16:56
 * Description: APIs for LatLng static and instances.
 * LatLng instance is a geo coordinates.
 */

@js.native
@JSName("L.LatLng")
object LatLng extends js.Object {

  val DEG_TO_RAD: Double = js.native

  val RAD_TO_DEG: Double = js.native

  val MAX_MARGIN: Double = js.native

}


@js.native
trait ILatLng extends js.Object {

  val lat: Double = js.native

  val lng: Double = js.native

}


@js.native
class LatLng extends ILatLng {

  def distanceTo(other: LatLng): Double = js.native

  def equals(other: LatLng): Boolean = js.native

  override def toString(): String = js.native

  def wrap(left: Double, right: Double): LatLng = js.native

}
