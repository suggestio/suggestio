package io.suggest.sjs.leaflet.layer.group

import io.suggest.sjs.leaflet.map.{ILayer, LMap}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 22:49
 * Description: API for layer groups.
 */
@js.native
@JSName("L.LayerGroup")
class LayerGroup extends js.Object {

  def addTo(lmap: LMap): this.type = js.native

  def addLayer(layer: ILayer): this.type = js.native

  def removeLayer(layer: ILayer): this.type = js.native

  def removeLayer(id: String): this.type = js.native

  def hasLayer(layer: ILayer): Boolean = js.native

  def getLayer(id: String): ILayer = js.native

  def getLayers(): js.Array[ILayer] = js.native

  def clearLayers(): this.type = js.native

  def eachLayer(f: js.Function1[ILayer, _], context: js.Object = js.native): this.type = js.native

  def toGeoJSON(): js.Object = js.native

}
