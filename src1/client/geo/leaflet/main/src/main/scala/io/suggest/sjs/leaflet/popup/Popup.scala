package io.suggest.sjs.leaflet.popup

import io.suggest.sjs.leaflet.event.LEventTarget
import io.suggest.sjs.leaflet.map.{LMap, LatLng}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:41
 * Description: API for popup instances.
 */

@JSImport("leaflet", "Popup")
@js.native
class Popup extends LEventTarget {

  def addTo(lmap: LMap): this.type = js.native

  def openOn(lmap: LMap): this.type = js.native

  def setLatLng(latLng: LatLng): this.type = js.native

  def getLatLng(): LatLng = js.native

  def setContent(htmlContent: String | HTMLElement): this.type = js.native

  def getContent(): String | HTMLElement = js.native

  def update(): this.type = js.native

}
