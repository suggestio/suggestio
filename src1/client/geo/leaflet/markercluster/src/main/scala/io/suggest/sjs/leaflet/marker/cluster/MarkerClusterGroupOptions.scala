package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.map.Zoom_t
import io.suggest.sjs.leaflet.path.PathOptions
import io.suggest.sjs.leaflet.path.poly.PolylineOptions

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 19:28
  * Description: API for [[MarkerClusterGroup]] constructor: L.markerClusterGroup(options).
  */
trait MarkerClusterGroupOptions extends js.Object {

  // Enabled by default (boolean options)

  val showCoverageOnHover         : UndefOr[Boolean]    = js.undefined

  val zoomToBoundsOnClick         : UndefOr[Boolean]    = js.undefined

  val spiderfyOnMaxZoom           : UndefOr[Boolean]    = js.undefined

  val removeOutsideVisibleBounds  : UndefOr[Boolean]    = js.undefined

  val animate                     : UndefOr[Boolean]    = js.undefined


  // Other options

  val animateAddingMarkers        : UndefOr[Boolean]            = js.undefined
  val disableClusteringAtZoom     : UndefOr[Zoom_t]             = js.undefined
  val maxClusterRadius            : UndefOr[Int | js.Function1[Int, Int]] = js.undefined
  val polygonOptions              : UndefOr[PathOptions]        = js.undefined
  val singleMarkerMode            : UndefOr[Boolean]            = js.undefined
  val spiderLegPolylineOptions    : UndefOr[PolylineOptions]    = js.undefined
  val spiderfyDistanceMultiplier  : UndefOr[Int]                = js.undefined
  val iconCreateFunction          : UndefOr[js.Function1[js.Object, js.Object]] = js.undefined
  val clusterPane                 : UndefOr[js.Any]             = js.undefined

}
