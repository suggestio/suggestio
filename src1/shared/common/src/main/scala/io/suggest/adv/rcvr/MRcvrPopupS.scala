package io.suggest.adv.rcvr

import boopickle.Default._
import io.suggest.geo.MGeoPoint


/** Состояние попапа над ресивером на карте. */
case class MRcvrPopupS(
  nodeId  : String,
  latLng  : MGeoPoint
)


object MRcvrPopupS {

  implicit val pickler: Pickler[MRcvrPopupS] = {
    implicit val mgpPickler = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[MRcvrPopupS]
  }

}