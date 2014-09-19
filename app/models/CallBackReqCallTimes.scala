package models

import play.api.data._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.09.14 14:52
 * Description: Доступное время для звонка в запросах обратного звонка.
 */

object CallBackReqCallTimes extends Enumeration {

  def formatIntTime(time: Int): String = {
    val hours = time / 100
    val minutes = time - (hours * 100)
    val minutesStr = String.format("%02d", Integer.valueOf(minutes))
    s"$hours:$minutesStr"
  }

  def unformatIntTime(time: String): Option[Int] = {
    time.split(":") match {
      case Array(hstr, mstr) =>
        val itime = (hstr + mstr).toInt
        Some(itime)
      case _ => None
    }
  }

  /**
   * Класс значения этого enum'а.
   * @param timeStart "12:00" -> 1200
   * @param timeEnd "18:30" -> 1830
   */
  protected case class Val(timeStart: Int, timeEnd: Int) extends super.Val() {
    def formatTimeStart = formatIntTime(timeStart)
    def formatTimeEnd = formatIntTime(timeEnd)

    def formatTime = formatTimeStart + " - " + formatTimeEnd
  }

  type CallBackReqCallTime = Val

  val MORNING: CallBackReqCallTime    = Val(1000, 1400)
  val AFTERNOON: CallBackReqCallTime  = Val(1400, 1800)

  implicit def value2val(x: Value): CallBackReqCallTime = x.asInstanceOf[CallBackReqCallTime]

  def fromString(s: String): Option[CallBackReqCallTime] = {
    s.split("\\s*-\\s*") match {
      case Array(startTime, endTime) =>
        unformatIntTime(startTime).flatMap { iStartTime =>
          unformatIntTime(endTime).flatMap { iEndTime =>
            forTimes(iStartTime, endTime = iEndTime)
          }
        }

      case _ => None
    }
  }

  def forTimes(startTime: Int, endTime: Int): Option[CallBackReqCallTime] = {
    values
      .find { v =>
        val cbrct: CallBackReqCallTime = v
        cbrct.timeStart == startTime  &&  cbrct.timeEnd == endTime
      }
      .asInstanceOf[Option[CallBackReqCallTime]]
  }

  /** Маппинг для формы (обязательный). */
  def mapping: Mapping[CallBackReqCallTime] = {
    import Forms._
    nonEmptyText(minLength = 6, maxLength = 20)
      .transform [Option[CallBackReqCallTime]] (
        { fromString }, { _.fold("")(_.formatTime) }
      )
      .verifying("error.required", _.isDefined)
      .transform [CallBackReqCallTime] (_.get, Some.apply)
  }

}
