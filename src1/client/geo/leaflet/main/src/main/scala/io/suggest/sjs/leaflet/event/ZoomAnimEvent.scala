package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.map.{LatLng, Zoom_t}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 23:45
  * @see [[https://leafletjs.com/reference-1.6.0.html#zoomanimevent]]
  */
@js.native
trait ZoomAnimEvent extends Event {

  val center: LatLng = js.native

  val zoom: Zoom_t = js.native

  val noUpdate: Boolean = js.native

}
