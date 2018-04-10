package io.suggest.sjs.leaflet.path

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.map.{LMap, LatLng, LatLngBounds, Layer}
import io.suggest.sjs.leaflet.popup.{Popup, PopupOptions}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:38
 * Description: Basic API for paths.
 */
@JSImport(LEAFLET_IMPORT, "Path")
@js.native
class Path extends Layer {

  def addTo(lmap: LMap): this.type = js.native

  def bindPopup(html: String | HTMLElement | Popup,  options: PopupOptions = js.native): this.type = js.native

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
