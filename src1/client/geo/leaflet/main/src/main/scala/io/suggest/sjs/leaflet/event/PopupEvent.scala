package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.popup.Popup
import io.suggest.sjs.leaflet.tooltip.Tooltip

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


@js.native
trait TooltipEvent extends Event {

  val tooltip: Tooltip = js.native

}
