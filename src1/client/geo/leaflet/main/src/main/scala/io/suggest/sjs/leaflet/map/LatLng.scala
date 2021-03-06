package io.suggest.sjs.leaflet.map

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 16:56
 * Description: APIs for LatLng static and instances.
 * LatLng instance is a geo coordinates.
 */
@JSImport(LEAFLET_IMPORT, "LatLng")
@js.native
object LatLng extends js.Object {

  val DEG_TO_RAD: Double = js.native

  val RAD_TO_DEG: Double = js.native

  val MAX_MARGIN: Double = js.native

}


trait LatLngLiteral extends js.Object {
  val lat: Double
  val lng: Double
}

@js.native
trait ILatLng extends js.Object {

  val lat: Double = js.native

  val lng: Double = js.native

}


@js.native
trait LatLng extends ILatLng {

  /** in meters */
  def distanceTo(other: LatLng): Double = js.native

  def equals(other: LatLng): Boolean = js.native

  override def toString: String = js.native

  def wrap(left: Double, right: Double): LatLng = js.native

}
