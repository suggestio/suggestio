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


  def interval = new Interval(dtStart, dtEnd)

}


case class MDateStartEnd(
  override val dateStart  : LocalDate,
  override val dateEnd    : LocalDate
)
  extends IDateStartEnd
