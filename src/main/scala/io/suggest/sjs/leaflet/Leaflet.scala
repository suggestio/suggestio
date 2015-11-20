package io.suggest.sjs.leaflet

import io.suggest.sjs.leaflet.control.LControl
import io.suggest.sjs.leaflet.map.{ILayer, Point, LatLng, LMap}
import io.suggest.sjs.leaflet.path.PathOptions
import io.suggest.sjs.leaflet.path.circle.Circle
import io.suggest.sjs.leaflet.popup.{Popup, PopupOptions}
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

  def map(target: MapTarget, options: js.Object = js.native): LMap = js.native

  def tileLayer(urlTemplate: String, options: TlOptions = js.native): TileLayer = js.native

  /** Geo coordinates. */
  def latLng(lat: Double, lng: Double): LatLng = js.native

  /** Pixel point. */
  def point(x: Int, y: Int): Point = js.native

  /** Controls. */
  def control: LControl = js.native

  /** Instantiate a circle. */
  def circle(latLng: LatLng, radiusMeters: Double, pathOptions: PathOptions = js.native): Circle = js.native

  /** Instantiate a popup. */
  def popup(options: PopupOptions = js.native, source: ILayer = js.native): Popup = js.native

}
