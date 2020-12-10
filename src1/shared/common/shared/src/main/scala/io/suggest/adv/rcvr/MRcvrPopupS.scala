package io.suggest.adv.rcvr

import io.suggest.geo.MGeoPoint
import japgolly.univeq.UnivEq


/** Состояние попапа над ресивером на карте. */
case class MRcvrPopupS(
  nodeId  : String,
  latLng  : MGeoPoint
)


object MRcvrPopupS {

  @inline implicit def univEq: UnivEq[MRcvrPopupS] = UnivEq.derive

}