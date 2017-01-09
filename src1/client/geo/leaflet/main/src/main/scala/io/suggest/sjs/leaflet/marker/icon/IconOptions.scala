package io.suggest.sjs.leaflet.marker.icon

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.leaflet.map.Point

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 15:53
 * Description: API for icon options.
 */
object IconOptions extends FromDict {
  override type T = IconOptions
}


@js.native
trait IconOptions extends js.Object {

  var iconUrl         : String    = js.native

  var iconRetinaUrl   : String    = js.native

  var iconSize        : Point     = js.native

  var iconAnchor      : Point     = js.native

  var shadowUrl       : String    = js.native

  var shadowRetinaUrl : String    = js.native

  var shadowSize      : Point     = js.native

  var shadowAnchor    : Point     = js.native

  var popupAnchor     : Point     = js.native

  var className       : String    = js.native

}
