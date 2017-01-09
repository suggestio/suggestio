package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.marker.Marker

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.16 12:32
  * Description: Marker API extensions from MarkerOpacity.js
  * @see [[https://github.com/Leaflet/Leaflet.markercluster/blob/master/src/MarkerOpacity.js]]
  */
@js.native
trait MarkerOpacity extends Marker {

  def clusterHide(): this.type = js.native

  def clusterShow(): this.type = js.native

}



/**
  * @see [[https://github.com/Leaflet/Leaflet.markercluster/blob/master/src/MarkerClusterGroup.Refresh.js#L85]]
  */
@js.native
trait MarkerRefresh extends Marker {

  def refreshIconOptions(options: js.Object, directlyRefreshClusters: Boolean = js.native): this.type = js.native

}
