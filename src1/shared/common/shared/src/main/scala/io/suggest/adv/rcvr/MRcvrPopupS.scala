package io.suggest.adv.rcvr

import boopickle.Default._
import io.suggest.geo.MGeoPoint
import japgolly.univeq.UnivEq


/** Состояние попапа над ресивером на карте. */
case class MRcvrPopupS(
  nodeId  : String,
  latLng  : MGeoPoint
)


object MRcvrPopupS {

  implicit def pickler: Pickler[MRcvrPopupS] = {
    implicit val mgpPickler = MGeoPoint.MGEO_POINT_PICKLER
    generatePickler[MRcvrPopupS]
  }

  @inline implicit def univEq: UnivEq[MRcvrPopupS] = UnivEq.derive

}