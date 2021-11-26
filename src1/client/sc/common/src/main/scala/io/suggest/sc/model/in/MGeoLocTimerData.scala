package io.suggest.sc.model.in

import io.suggest.sc.model.GeoLocTimerStart
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq.UnivEq

import scala.scalajs.js.timers.SetTimeoutHandle


object MGeoLocTimerData {
  @inline implicit def univEq: UnivEq[MGeoLocTimerData] = UnivEq.derive
}


/** Container of data about currently-running geolocation timer.
  *
  * @param timerId id of currently running timer.
  * @param reason timer data.
  */
final case class MGeoLocTimerData(
                                   timerId      : SetTimeoutHandle,
                                   reason       : GeoLocTimerStart,
                                 )
