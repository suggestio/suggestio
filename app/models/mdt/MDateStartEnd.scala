package models.mdt

import org.joda.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 18:47
 * Description: Интерфейс для моделей, описывающих период из двух дат.
 */
trait IDateStartEnd {

  /** Дата начала. */
  def dateStart: LocalDate

  /** Дата окончания. */
  def dateEnd: LocalDate

}


case class MDateStartEnd(
  override val dateStart  : LocalDate,
  override val dateEnd    : LocalDate
)
  extends IDateStartEnd
