package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.popup.Popup

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 14:59
 * Description: API for popup events.
 */

@js.native
trait PopupEvent extends Event {

  var popup: Popup = js.native

}
