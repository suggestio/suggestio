package io.suggest.sjs.mapbox.gl.style

import io.suggest.sjs.mapbox.gl.Zoom_t

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:04
  * Description: A subset of Map, with addLayer, removeLayer, setPaintProperty,
  * setLayoutProperty, setFilter, setLayerZoomRange, addSource, and removeSource.
  */

@js.native
trait StyleBatch extends js.Object {

  def addLayer(layer: js.Object, before: String = js.native): this.type = js.native
  def removeLayer(id: String): this.type = js.native

  def addSource(id: String, source: js.Object): this.type = js.native
  def removeSource(id: String): this.type = js.native

  def setFilter(layerId: String, filter: js.Array[js.Any]): this.type = js.native
  def setLayerZoomRange(layerId: String, minzoom: Zoom_t, maxzoom: Zoom_t): this.type = js.native
  def setLayoutProperty(layerId: String, name: String, value: js.Any): this.type = js.native
  def setPaintProperty(layerId: String, name: String, value: js.Any, klass: String = js.native): this.type = js.native

}
