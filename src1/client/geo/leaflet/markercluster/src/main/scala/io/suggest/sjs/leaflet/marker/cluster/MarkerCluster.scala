package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.map.LatLngBounds
import io.suggest.sjs.leaflet.marker.Marker
import io.suggest.sjs.leaflet.marker.icon.Icon

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.16 12:24
  * Description: MarkerCluster API.
  */
@JSImport("leaflet", "MarkerCluster")
@js.native
class MarkerCluster extends Marker {

  def getChildCount(): Int = js.native

  def getAllChildMarkers(storageArray: js.Array[_] = js.native): js.Array[Marker] = js.native

  def zoomToBounds(): Unit = js.native

  def getBounds(): LatLngBounds = js.native

  def createIcon(): Icon = js.native

  def createShadow(): js.Object = js.native


  // MarkerCluster.Spiderfier.js
  def spiderfy(): Unit = js.native

  def getConvexHull(): js.Object = js.native

}

@JSImport("leaflet", "MarkerClusterNonAnimated")
@js.native
class MarkerClusterNonAnimated extends MarkerCluster
