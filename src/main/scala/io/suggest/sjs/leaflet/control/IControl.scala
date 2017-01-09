package io.suggest.sjs.leaflet.control

import io.suggest.sjs.leaflet.map.{Layer, LMap}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 16:37
 * Description: API for controls.
 * @see [[http://leafletjs.com/reference.html#icontrol]]
 */
@js.native
@JSName("L.Control")
class IControl extends Layer {

  //var position: String = js.native

  def getPosition(): String = js.native

  def setPosition(pos: String): this.type = js.native

  def getContainer(): HTMLElement = js.native

  def addTo(lmap: LMap): this.type = js.native

  def remove(): js.native = js.native

}
