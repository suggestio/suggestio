package io.suggest.sjs.leaflet.marker

import io.suggest.sjs.leaflet.Leaflet
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.11.16 11:30
  */
package object cluster {

  /** L.* API extensions. */
  implicit def l2lmcApi(l: Leaflet.type): LeafletMarkerCluster = {
    l.asInstanceOf[LeafletMarkerCluster]
  }

  implicit def markerOpacity(m: Marker): MarkerOpacity = {
    m.asInstanceOf[MarkerOpacity]
  }

  implicit def markerRefresh(m: Marker): MarkerRefresh = {
    m.asInstanceOf[MarkerRefresh]
  }

}
