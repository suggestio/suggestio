package io.suggest.sjs.leaflet.event

import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 23:42
  * @see [[https://leafletjs.com/reference-1.6.0.html#keyboardevent-originalevent]]
  */
@js.native
trait KeyboardEvent extends Event {

  val originalEvent: dom.KeyboardEvent = js.native

}
