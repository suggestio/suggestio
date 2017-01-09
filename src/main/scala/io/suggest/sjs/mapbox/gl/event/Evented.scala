package io.suggest.sjs.mapbox.gl.event

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:26
  * Description: API for evented mix-in.
  */
@js.native
@JSName("mapboxgl.Evented")
trait Evented extends js.Object {

  def fire(etype: String, eventData: EventData = js.native): this.type = js.native

  def listens(etype: String): Boolean = js.native

  def off(etype: String = js.native, f: Listener_t = js.native): this.type = js.native

  def on(etype: String, f: Listener_t): this.type = js.native

  def once(etype: String, f: Listener_t): this.type = js.native

}
