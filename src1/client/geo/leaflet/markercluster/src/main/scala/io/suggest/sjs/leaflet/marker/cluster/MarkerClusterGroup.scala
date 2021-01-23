package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.event.Evented
import io.suggest.sjs.leaflet.layer.group.FeatureGroup
import io.suggest.sjs.leaflet.map.Layer
import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 19:22
  * Description: API for marker cluster layer.
  *
  * Из-за проблем с CommonJS [[https://github.com/Leaflet/Leaflet.markercluster/issues/441]]
  * тут костыли shim'а: подмена import'ов и перехват export'ов на уровне webpack.
  */

object MarkerClusterGroup {

  def apply(options: MarkerClusterGroupOptions): MarkerClusterGroup = {
    new MarkerClusterGroup(options)
  }

}


@JSImport("imports-loader?L=leaflet!exports-loader?L.MarkerClusterGroup!./node_modules/leaflet.markercluster/dist/leaflet.markercluster.js", JSImport.Namespace)
@js.native
sealed class MarkerClusterGroup(options: MarkerClusterGroupOptions)
  extends FeatureGroup
  with Evented
{

  def initialize(options: MarkerClusterGroupOptions): Unit = js.native

  def addLayers[T <: Layer](layers: js.Array[T]): this.type = js.native

  def refreshClusters(what: js.Any = js.native): this.type = js.native

  def removeLayers(layers: js.Array[Layer]): this.type = js.native

  /** @return null || layer */
  def getLayer(id: Int): Layer = js.native

  def zoomToShowLayer(layer: Layer, callback: js.Function0[_] = js.native): Unit = js.native

  /** @return null || layer */
  def getVisibleParent(marker: Layer): Layer = js.native

}

