package io.suggest.sjs.leaflet.path

import io.suggest.sjs.leaflet.map.{LatLngBounds, LatLng, LMap}
import io.suggest.sjs.leaflet.popup.{PopupOptions, Popup}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:38
 * Description: Basic API for paths.
 */

@js.native
@JSName("L.Path")
class Path extends js.Object {

  def addTo(lmap: LMap): this.type = js.native

  def bindPopup(html: String | HTMLElement | Popup,  options: PopupOptions): this.type = js.native

  def unbindPopup(): this.type = js.native

  def openPopup(latLng: LatLng = js.native): this.type = js.native

  def closePopup(): this.type = js.native

  def setStyle(options: PathOptions): this.type = js.native

  def getBounds(): LatLngBounds = js.native

  def bringToFront(): this.type = js.native

  def bringToBack(): this.type = js.native

  def redraw(): this.type = js.native

  var SVG: Boolean = js.native

  var VML: Boolean = js.native

  var CANVAS: Boolean = js.native

  var CLIP_PADDING: Double = js.native

}
