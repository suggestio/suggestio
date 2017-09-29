package io.suggest.sjs.leaflet.marker.cluster

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 19:28
  * Description: API for [[MarkerClusterGroup]] constructor: L.markerClusterGroup(options).
  */
trait MarkerClusterGroupOptions extends js.Object {

  val showCoverageOnHover         : UndefOr[Boolean]    = js.undefined

  val zoomToBoundsOnClick         : UndefOr[Boolean]    = js.undefined

  val spiderfyOnMaxZoom           : UndefOr[Boolean]    = js.undefined

  val removeOutsideVisibleBounds  : UndefOr[Boolean]    = js.undefined

  val spiderLegPolylineOptions    : UndefOr[js.Object]  = js.undefined

}
