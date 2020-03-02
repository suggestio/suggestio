package cordova.plugins.notification.local

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.2020 10:12
  * Description: notification.actions[].trigger
  *
  * @see [[https://github.com/katzer/cordova-plugin-local-notifications#triggers]]
  */
object CnlTrigger {

  protected trait Units0[T <: js.Any] {
    def minute = "minute".asInstanceOf[T]
    def hour = "hour".asInstanceOf[T]
    def day = "day".asInstanceOf[T]
    def week = "week".asInstanceOf[T]
    def month = "month".asInstanceOf[T]
    def quarter = "quarter".asInstanceOf[T]
    def year = "year".asInstanceOf[T]
  }


  type TimespanUnit_t <: js.Any
  object TimespanUnits extends Units0[TimespanUnit_t] {
    def second = "second".asInstanceOf[TimespanUnit_t]
  }


  type RepeatEvery_t <: TimespanUnit_t
  object RepeatEvery extends Units0[RepeatEvery_t]


  type MatchEvery_t <: TimespanUnit_t
  object MatchEvery extends Units0[MatchEvery_t] {
    def weekday = "weekday".asInstanceOf[MatchEvery_t]
    def weekdayOrdinal = "weekdayOrdinal".asInstanceOf[MatchEvery_t]
    def weekOfMonth = "weekOfMonth".asInstanceOf[MatchEvery_t]
  }


  type Type_t <: js.Any
  object Types {
    def fix = "fix".asInstanceOf[Type_t]
    def timespan = "timespan".asInstanceOf[Type_t]
    def repeat = "repeat".asInstanceOf[Type_t]
    def `match` = "match".asInstanceOf[Type_t]
    def location = "location".asInstanceOf[Type_t]
  }

}
trait CnlTrigger extends js.Object {
  val `type`: js.UndefOr[CnlTrigger.Type_t] = js.undefined
  // type = fix
  val at: js.UndefOr[js.Date] = js.undefined

  // type = timespan
  val in: js.UndefOr[Int] = js.undefined
  val unit: js.UndefOr[CnlTrigger.TimespanUnit_t] = js.undefined

  // type = repeat | match
  val every: js.UndefOr[CnlTrigger.RepeatEvery_t | CnlTrigger.MatchEvery_t | CnlTriggerMatchEvery] = js.undefined
  val count: js.UndefOr[Int] = js.undefined
  val before: js.UndefOr[js.Date] = js.undefined
  // type = repeat
  val firstAt: js.UndefOr[js.Date] = js.undefined
  // type = match
  val after: js.UndefOr[js.Date] = js.undefined

  // type = location  // iOS only
  val center: js.UndefOr[js.Array[Double]] = js.undefined  // [lat, long]
  val radius: js.UndefOr[Int] = js.undefined
  val notifyOnEntry: js.UndefOr[Boolean] = js.undefined
  val notifyOnExit: js.UndefOr[Boolean] = js.undefined
  val single: js.UndefOr[Boolean] = js.undefined
}


/** trigger type=match every={...} */
trait CnlTriggerMatchEvery extends js.Object {
  val month: js.UndefOr[Int] = js.undefined
  val day: js.UndefOr[Int] = js.undefined
  val hour: js.UndefOr[Int] = js.undefined
  val minute: js.UndefOr[Int] = js.undefined
}
