package io.suggest.sjs.leaflet.marker

import io.suggest.sjs.leaflet.event.LEventTarget
import io.suggest.sjs.leaflet.handler.IHandler
import io.suggest.sjs.leaflet.map.{LatLng, LMap}
import io.suggest.sjs.leaflet.marker.icon.Icon
import io.suggest.sjs.leaflet.popup.{PopupOptions, Popup}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 15:40
 * Description: Маркер на карте.
 */

@js.native
@JSName("L.Marker")
class Marker extends LEventTarget {

  def addTo(lmap: LMap): this.type = js.native

  def getLatLng(): LatLng = js.native

  def setLatLng(latLng: LatLng): this.type = js.native

  def setIcon(icon: Icon): this.type = js.native

  def setZIndexOffset(zOff: Int): this.type = js.native

  def setOpacity(opacity: Double): this.type = js.native

  def update(): this.type = js.native

  def bindPopup(html: String | HTMLElement | Popup, options: PopupOptions): this.type = js.native

  def unbindPopup(): this.type = js.native

  def openPopup(): this.type = js.native

  def getPopup(): Popup = js.native

  def closePopup(): this.type = js.native

  def togglePopup(): this.type = js.native

  def setPopupContent(html: String | HTMLElement): this.type = js.native

  def toGeoJSON(): js.Object = js.native


  var dragging: IHandler = js.native

}
