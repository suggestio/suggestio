package io.suggest.sjs.leaflet.event

import io.suggest.sjs.leaflet.map.{Point, LatLng}

import scala.scalajs.js
import org.scalajs.dom.{raw => sjs}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 14:49
  * Description:
  */
@js.native
trait MouseEvent extends Event {

  var latlng: LatLng = js.native

  var layerPoint: Point = js.native

  var containerPoint: Point = js.native

  var originalEvent: sjs.MouseEvent = js.native

}
