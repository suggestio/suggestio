package io.suggest.sjs.leaflet.marker.cluster

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 19:28
  * Description: API for [[MarkerClusterGroup]] constructor: L.markerClusterGroup(options).
  */
@js.native
class MarkerClusterGroupOptions extends js.Object {

  var showCoverageOnHover: Boolean = js.native

  var zoomToBoundsOnClick: Boolean = js.native

  var spiderfyOnMaxZoom: Boolean = js.native

  var removeOutsideVisibleBounds: Boolean = js.native

  var spiderLegPolylineOptions: js.Object = js.native

}
