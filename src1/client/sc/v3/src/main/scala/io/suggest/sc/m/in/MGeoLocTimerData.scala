package io.suggest.sc.m.in

import io.suggest.sc.m.GeoLocTimerStart
import japgolly.univeq.UnivEq


object MGeoLocTimerData {
  @inline implicit def univEq: UnivEq[MGeoLocTimerData] = UnivEq.derive
}


/** Container of data about currently-running geolocation timer.
  *
  * @param timerId id of currently running timer.
  * @param reason timer data.
  */
final case class MGeoLocTimerData(
                                   timerId      : Int,
                                   reason       : GeoLocTimerStart,
                                 )
