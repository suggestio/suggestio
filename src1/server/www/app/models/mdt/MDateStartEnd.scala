package models.mdt

import io.suggest.dt.{IPeriodInfo, IYmdHelper}
import org.joda.time.{Interval, LocalDate}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 18:47
 * Description: Интерфейс для моделей, описывающих период из двух дат.
 */
trait IDateStartEndJ8t {
  def dateStart: java.time.LocalDate
  def dateEnd: java.time.LocalDate
}
case class MDateStartEndJ8t(
                           override val dateStart : java.time.LocalDate,
                           override val dateEnd   : java.time.LocalDate
                         )
  extends IDateStartEndJ8t
object MDateStartEndJ8t {
  def apply(m: IPeriodInfo)(implicit ymdHelper: IYmdHelper[LocalDate]): MDateStartEnd = {
    MDateStartEnd(
      dateStart = m.dateStart[LocalDate],
      dateEnd   = m.dateEnd[LocalDate]
    )
  }
}


trait IDateStartEnd {

  /** Дата начала. */
  def dateStart: LocalDate

  def dtStart = dateStart.toDateTimeAtStartOfDay


  /** Дата окончания. */
  def dateEnd: LocalDate

  def dtEnd = dateEnd.toDateTimeAtStartOfDay


  def interval: Interval = {
    new Interval(dtStart, dtEnd)
  }

  override def toString: String = s"$dateStart..$dateEnd"
}


object MDateStartEnd {

  def apply(m: IPeriodInfo)(implicit ymdHelper: IYmdHelper[LocalDate]): MDateStartEnd = {
    MDateStartEnd(
      dateStart = m.dateStart[LocalDate],
      dateEnd   = m.dateEnd[LocalDate]
    )
  }

}

/** Дефолтовая реализация [[IDateStartEnd]]. */
case class MDateStartEnd(override val dateStart : LocalDate,
                         override val dateEnd   : LocalDate)
  extends IDateStartEnd
{

  override def toString = super.toString

}
