package io.suggest.maps.m

import io.suggest.geo.MGeoPoint
import io.suggest.maps.u.MapsUtil
import japgolly.univeq.UnivEq


/** Состояние попапа над ресивером на карте. */
case class MRcvrPopupS(
                        nodeId  : String,
                        geoPoint  : MGeoPoint
                      ) {

  lazy val leafletLatLng = MapsUtil.geoPoint2LatLng( geoPoint )

}


object MRcvrPopupS {

  @inline implicit def univEq: UnivEq[MRcvrPopupS] = UnivEq.derive

}