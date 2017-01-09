package io.suggest.sjs.leaflet.marker

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.leaflet.marker.icon.Icon

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 15:47
 * Description: API for marker options.
 */
object MarkerOptions extends FromDict {
  override type T = MarkerOptions
}


@js.native
trait MarkerOptions extends js.Object {

  var icon        : Icon    = js.native

  var clickable   : Boolean = js.native

  var draggable   : Boolean = js.native

  var keyboard    : Boolean = js.native

  var title       : String  = js.native

  var alt         : String  = js.native

  var zIndexOffset: Int     = js.native

  var opacity     : Double  = js.native

  var riseOnHover : Boolean = js.native

  var riseOffset  : Int     = js.native

}
