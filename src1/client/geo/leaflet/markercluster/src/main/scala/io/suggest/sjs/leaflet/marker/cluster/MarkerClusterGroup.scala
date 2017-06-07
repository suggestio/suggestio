package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.event.LEventTarget
import io.suggest.sjs.leaflet.layer.group.FeatureGroup
import io.suggest.sjs.leaflet.map.Layer

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobal, JSImport}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 19:22
  * Description: API for marker cluster layer.
  * Из-за проблем с CommonJS [[https://github.com/Leaflet/Leaflet.markercluster/issues/441]]
  * тут костыль, имитирующий
  * {{{
  *   L = require("leaflet");
  *   require("leaflet.markercluster");
  *   return L.markerClusterGroup(...);
  * }}}
  */

object MarkerClusterGroup {

  {
    Leaflet
    try {
      // Должна быть ошибка, т.к. мы импортируем тут не-commonJS-модуль:
      new McgRequireWrap
    } catch { case _: Throwable =>
      // do nothing - подавить ошибку, т.к. она вполне ожидаема
    }
  }

  def apply(options: MarkerClusterGroupOptions): MarkerClusterGroup = {
    Leaflet.markerClusterGroup(options)
  }

}


//@JSImport("leaflet.markercluster", ???)
@JSGlobal("L.MarkerClusterGroup")
@js.native
sealed class MarkerClusterGroup(options: MarkerClusterGroupOptions) extends FeatureGroup with LEventTarget {

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

@JSImport("leaflet.markercluster", JSImport.Namespace)
@js.native
sealed class McgRequireWrap extends js.Object
