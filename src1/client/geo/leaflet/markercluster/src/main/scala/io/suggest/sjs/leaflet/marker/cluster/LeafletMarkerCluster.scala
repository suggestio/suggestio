package io.suggest.sjs.leaflet.marker.cluster

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.16 11:27
  * Description: Leaflet API extensions for markercluster plugin.
  */
@js.native
trait LeafletMarkerCluster extends js.Object {

  def markerClusterGroup(mcgOptions: MarkerClusterGroupOptions = js.native): MarkerClusterGroup = js.native

}
