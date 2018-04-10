package io.suggest.sjs.leaflet.control

import io.suggest.sjs.leaflet.map.{LMap, Layer}
import org.scalajs.dom.raw.HTMLElement
import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 16:37
 * Description: API for controls.
 * @see [[http://leafletjs.com/reference.html#icontrol]]
 */
@JSImport(LEAFLET_IMPORT, "Control")
@js.native
class IControl extends Layer {

  //var position: String = js.native

  def getPosition(): String = js.native

  def setPosition(pos: String): this.type = js.native

  def getContainer(): HTMLElement = js.native

  def addTo(lmap: LMap): this.type = js.native

  def remove(): js.native = js.native

}
