package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.map.Layer

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.16 21:20
  * Description: API for layer events.
  * @see [[http://leafletjs.com/reference-1.0.2.html#layer]]
  */
@js.native
class LayerEvent extends Event {

  val layer: Layer = js.native

}
