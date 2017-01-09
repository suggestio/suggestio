package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 19:03
 * Description: Map panning options model.
 */
@js.native
sealed trait PanOptions extends js.Object {

  var animate:  Boolean = js.native

  var duration: 	Double = js.native

  var easeLinearity: Double = js.native

  var noMoveStart: Boolean = js.native

}
