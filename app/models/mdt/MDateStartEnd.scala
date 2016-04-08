package models.mdt

import org.joda.time.{Interval, LocalDate}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 18:47
 * Description: Интерфейс для моделей, описывающих период из двух дат.
 */
trait IDateStartEnd {

  /** Дата начала. */
  def dateStart: LocalDate

  def dtStart = dateStart.toDateTimeAtStartOfDay


  /** Дата окончания. */
  def dateEnd: LocalDate

  def dtEnd = dateEnd.toDateTimeAtStartOfDay


  def interval: Interval

}

trait MDateStartEndT extends IDateStartEnd {
  override def interval: Interval = {
    new Interval(dtStart, dtEnd)
  }
}

/** Реализация [[IDateStartEnd]] для двух joda-дат. */
case class MDateStartEnd(
  override val dateStart  : LocalDate,
  override val dateEnd    : LocalDate
)
  extends MDateStartEndT


/** Реализация [[IDateStartEnd]] для одного joda-интервала. */
case class MDateInterval(override val interval: Interval) extends IDateStartEnd {

  override def dateStart: LocalDate = {
    dtStart.toLocalDate
  }
  override def dtStart = interval.getStart

  override def dateEnd: LocalDate = {
    dtEnd.toLocalDate
  }
  override def dtEnd = interval.getEnd

}