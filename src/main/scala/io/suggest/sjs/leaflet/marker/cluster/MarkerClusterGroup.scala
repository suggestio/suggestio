package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.event.LEventTarget
import io.suggest.sjs.leaflet.layer.group.FeatureGroup
import io.suggest.sjs.leaflet.map.ILayer

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 19:22
  * Description: API for marker cluster layer.
  */
@js.native
class MarkerClusterGroup extends FeatureGroup with LEventTarget {

  def initialize(options: MarkerClusterGroupOptions): Unit = js.native

  def addLayers[T <: ILayer](layers: js.Array[T]): this.type = js.native

  def refreshClusters(what: js.Any = js.native): this.type = js.native

  def removeLayers(layers: js.Array[ILayer]): this.type = js.native

  /** @return null || layer */
  def getLayer(id: Int): ILayer = js.native

  def zoomToShowLayer(layer: ILayer, callback: js.Function0[_] = js.native): Unit = js.native

  /** @return null || layer */
  def getVisibleParent(marker: ILayer): ILayer = js.native

}
