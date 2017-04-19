package io.suggest.maps.u

import io.suggest.geo.MGeoPoint
import io.suggest.sjs.common.model.loc.MGeoPointJs
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.sjs.leaflet.map.LatLng

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.17 22:21
  * Description: JS-утиль для работы с географическими картами и геоданными.
  */
object MapsUtil {


  /** Конверсия L.LatLng в MGeoPoint. */
  def latLng2geoPoint(ll: LatLng): MGeoPoint = {
    MGeoPoint(
      lat = ll.lat,
      lon = ll.lng
    )
  }


  /** Конверсия MGeoPoint в L.LatLng. */
  def geoPoint2LatLng(gp: MGeoPoint): LatLng = {
    Leaflet.latLng( MGeoPointJs.toLatLngArray(gp) )
  }


  /** Посчитать расстояние между двумя точками. */
  def distanceBetween(gp0: MGeoPoint, gp1: MGeoPoint): Double = {
    geoPoint2LatLng(gp0)
      .distanceTo( geoPoint2LatLng(gp1) )
  }

}
