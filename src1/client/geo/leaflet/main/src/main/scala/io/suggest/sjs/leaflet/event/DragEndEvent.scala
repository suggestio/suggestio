package io.suggest.sjs.leaflet.event

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:58
 * Description: API for `dragent` event.
 */
@js.native
trait DragEndEvent extends Event {

  var distance: Double = js.native

}
