package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.map.Point

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 15:00
 * Description: API for resize events.
 */

@js.native
trait ResizeEvent extends Event {

  var oldSize: Point = js.native

  var newSize: Point = js.native

}
