package io.suggest.sjs.leaflet.tilelayer

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import io.suggest.sjs.leaflet.map.LMap

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 18:26
 * Description: API for L.TileLayer model.
 */
@JSImport(LEAFLET_IMPORT, "TileLayer")
@js.native
class TileLayer extends js.Object {

  def addTo(map: LMap): TileLayer = js.native

  def bringToFront(): TileLayer = js.native

  def bringToBack(): TileLayer = js.native

  def setOpacity(opacity: Double): TileLayer = js.native

  def setZIndex(zIndex: Double): TileLayer = js.native

  def redraw(): TileLayer = js.native

  def setUrl(urlTemplate: String): TileLayer = js.native

  def getContainer(): HTMLElement = js.native

}
