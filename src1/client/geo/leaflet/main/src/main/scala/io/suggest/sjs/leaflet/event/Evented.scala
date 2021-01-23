package io.suggest.sjs.leaflet.event

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 11:30
 * Description: API for event targets.
 */
@js.native
trait Evented extends js.Object {

  @JSName("addEventListener")
  def addEventListener3[T <: Event](etype: String, f: js.Function1[T,_], ctx: js.Object = js.native): this.type = js.native
  @JSName("on")
  def on3[T <: Event](etype: String, f: js.Function1[T,_], ctx: js.Object = js.native): this.type = js.native

  @JSName("addEventListener")
  def addEventListener2(evtMap: js.Object, ctx: js.Object = js.native): this.type = js.native
  @JSName("on")
  def on2(evtMap: js.Object, ctx: js.Object = js.native): this.type = js.native


  def addOneTimeEventListener[T <: Event](etype: String, f: js.Function1[T,_], ctx: js.Object = js.native): this.type = js.native
  def once[T <: Event](etype: String, f: js.Function1[T,_], ctx: js.Object = js.native): this.type = js.native


  @JSName("removeEventListener")
  def removeEventListener3[T <: Event](etype: String, f: js.Function1[T,_], ctx: js.Object = js.native): this.type = js.native
  @JSName("off")
  def off3[T <: Event](etype: String, f: js.Function1[T,_], ctx: js.Object = js.native): this.type = js.native

  @JSName("removeEventListener")
  def removeEventListener2(evtMap: js.Object, ctx: js.Object = js.native): this.type = js.native
  @JSName("off")
  def off2(evtMap: js.Object, ctx: js.Object = js.native): this.type = js.native

  def removeEventListener(): this.type = js.native
  def off(): this.type = js.native
  def clearAllEventListeners(): this.type = js.native


  def hasEventListeners(etype: String): Boolean = js.native


  def fireEvent(etype: String, data: js.Object = js.native): this.type = js.native
  def fire(etype: String, data: js.Object = js.native): this.type = js.native

}
