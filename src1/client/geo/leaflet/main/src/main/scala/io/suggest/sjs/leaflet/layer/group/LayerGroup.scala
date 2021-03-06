package io.suggest.sjs.leaflet.layer.group

import io.suggest.sjs.leaflet.map.{LMap, Layer}
import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 22:49
 * Description: API for layer groups.
 */
@JSImport(LEAFLET_IMPORT, "LayerGroup")
@js.native
class LayerGroup extends Layer with ControlledLayer {

  def addTo(lmap: LMap): this.type = js.native

  def removeLayer(id: String): this.type = js.native

  def hasLayer(layer: Layer): Boolean = js.native

  def getLayer(id: String): Layer = js.native

  def getLayers(): js.Array[Layer] = js.native

  def clearLayers(): this.type = js.native

  def eachLayer(f: js.Function1[Layer, _], context: js.Any = js.native): this.type = js.native

  def toGeoJSON(): js.Object = js.native

}



/** react-leaflet's layer container interface. */
@js.native
trait ControlledLayer extends js.Object {
  def addLayer(layer: Layer): this.type = js.native
  def removeLayer(layer: Layer): this.type = js.native
}
