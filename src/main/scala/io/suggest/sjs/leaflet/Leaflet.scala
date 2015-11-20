package io.suggest.sjs.leaflet

import io.suggest.sjs.leaflet.control.LControl
import io.suggest.sjs.leaflet.map.{Point, LatLng, LMap}
import io.suggest.sjs.leaflet.tilelayer.{TileLayer, TlOptions}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:51
  * Description: Core interface of leaflet.
  */
@js.native
@JSName("L")
object Leaflet extends js.Object {

  def map(target: MapTarget, options: js.Object = null): LMap = js.native

  def tileLayer(urlTemplate: String, options: TlOptions = null): TileLayer = js.native

  /** Geo coordinates. */
  def latLng(lat: Double, lng: Double): LatLng = js.native

  /** Pixel point. */
  def point(x: Int, y: Int): Point = js.native

  /** Controls. */
  def control: LControl = js.native

}
