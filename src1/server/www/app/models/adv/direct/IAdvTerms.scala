package models.adv.direct

import java.time.LocalDate

import models.mdt.IDateStartEnd

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.11.15 16:15
 * Description: Условия размещения с точки зрения юзера.
 */
trait IAdvTerms extends IDateStartEnd {

  def dateStart: LocalDate
  def dateEnd: LocalDate

  // TODO Как-то так получилось, что на обе даты размещается включительно
  override def dtEnd   = dateEnd.plusDays(1).atStartOfDay().minusSeconds(1)

}
