package io.suggest.sjs.leaflet.layer.group

import io.suggest.sjs.leaflet.map.LatLngBounds
import io.suggest.sjs.leaflet.path.PathOptions
import io.suggest.sjs.leaflet.popup.PopupOptions
import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 22:56
 * Description: API for L.FeatureGroup instances.
 */
@JSImport(LEAFLET_IMPORT, "FeatureGroup")
@js.native
class FeatureGroup extends LayerGroup {

  def bindPopup(htmlContent: String, options: PopupOptions = js.native): this.type = js.native

  def getBounds(): LatLngBounds = js.native

  def setStyle(options: PathOptions): this.type = js.native

  def bringToFront(): this.type = js.native

  def bringToBack(): this.type = js.native

}
