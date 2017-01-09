package io.suggest.sjs.leaflet.map

import io.suggest.sjs.leaflet.event.LEventTarget

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 17:00
  * Description: Abstract Layer API.
  */
@js.native
@JSName("L.Layer")
class Layer extends LEventTarget {

  def onAdd(lmap: LMap): this.type = js.native

  def onRemove(lmap: LMap): this.type = js.native

  def getEvents(): js.Object = js.native

  def getAttribution(): String = js.native

  def beforeAdd(lmap: LMap): this.type = js.native

}
