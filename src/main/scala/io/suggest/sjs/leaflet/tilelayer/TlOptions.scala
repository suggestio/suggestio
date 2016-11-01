package io.suggest.sjs.leaflet.tilelayer

import io.suggest.sjs.leaflet.map.LatLngBounds
import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js
import scala.scalajs.js.`|`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 18:04
  * Description: Interface for TileLayer options model.
  */
object TlOptions extends FromDict {
  override type T = TlOptions
}

@js.native
class TlOptions extends js.Object {

  var minZoom: Double = js.native
  var maxZoom: Double = js.native
  var maxNativeZoom: Double = js.native
  var tileSize: Double = js.native
  var subdomains: String | js.Array[String] = js.native
  var errorTileUrl: String = js.native
  var attribution: String = js.native
  var tms: Boolean = js.native
  var continuousWorld: Boolean = js.native
  var noWrap: Boolean = js.native
  var zoomOffset: Double = js.native
  var zoomReverse: Boolean = js.native
  var opacity: Double = js.native
  var zIndex: Double = js.native
  var unloadInvisibleTiles: Boolean = js.native
  var updateWhenIdle: Boolean = js.native
  var detectRetina: Boolean = js.native
  var reuseTiles: Boolean = js.native
  var bounds: LatLngBounds = js.native

}
