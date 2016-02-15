package models.adv

import org.joda.time.{Interval, LocalDate}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 16:15
 * Description: Условия размещения с точки зрения юзера.
 */
trait IAdvTerms extends SinkShowLevelsFilters {
  def dateStart: LocalDate
  def dateEnd: LocalDate

  def dtStart = dateStart.toDateTimeAtStartOfDay
  // TODO Как-то так получилось, что на обе даты размещается включительно
  def dtEnd   = dateEnd.plusDays(1).toDateTimeAtStartOfDay.minusSeconds(1)

  def dtInterval = new Interval(dtStart, dtEnd)

}


trait AdvTermsWrapper extends IAdvTerms {
  def underlying: IAdvTerms

  override def dateStart = underlying.dateStart
  override def dateEnd = underlying.dateEnd
  override def showLevels = underlying.showLevels
}

