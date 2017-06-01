package io.suggest.sjs.leaflet.event

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:56
 * Description: API for Leaflet ErrorEvent.
 */
@js.native
trait ErrorEvent extends Event {

  var message: String = js.native

  var code: Double = js.native

}
