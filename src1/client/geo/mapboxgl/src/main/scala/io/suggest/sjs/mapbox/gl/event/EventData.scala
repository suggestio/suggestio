package io.suggest.sjs.mapbox.gl.event

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.ll.LngLat
import io.suggest.sjs.mapbox.gl.map.Point
import org.scalajs.dom.Event

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:32
  * Description: API for event data.
  */
object EventData extends FromDict {
  override type T = EventData
}

@js.native
class EventData extends js.Object {

  var originalEvent: Event = js.native

  var point: Point = js.native

  var lngLat: LngLat = js.native

}
