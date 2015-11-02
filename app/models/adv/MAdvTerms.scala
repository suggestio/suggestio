package models.adv

import org.joda.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 16:15
 * Description: Условия размещения с точки зрения юзера.
 */
trait IAdvTerms extends SinkShowLevelsFilters {
  def dateStart: LocalDate
  def dateEnd: LocalDate
}


trait AdvTermsWrapper extends IAdvTerms {
  def underlying: IAdvTerms

  override def dateStart = underlying.dateStart
  override def dateEnd = underlying.dateEnd
  override def showLevels = underlying.showLevels
}

