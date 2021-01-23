package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.map.Point
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 23:47
  * @see [[https://leafletjs.com/reference-1.6.0.html#tileevent]]
  */
@js.native
trait TileEvent extends Event {

  val tile: dom.html.Element = js.native

  val coords: Point = js.native

}


@js.native
trait TileErrorEvent extends TileEvent {

  val error: js.Any = js.native

}