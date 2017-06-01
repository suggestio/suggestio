package io.suggest.sjs.leaflet.marker

import io.suggest.sjs.leaflet.event.LayerEvent

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.16 21:48
  * Description: Marker event API facade.
  */
@js.native
trait MarkerEvent extends LayerEvent {

  override val layer: Marker = js.native

}
