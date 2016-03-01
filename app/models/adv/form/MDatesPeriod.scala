package models.adv.form

import models.mdt.IDateStartEnd
import org.joda.time.LocalDate

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 16:06
 * Description: Модель с описанием периода размещения на основе двух дат либо периода-презета.
 */

object MDatesPeriod {

  def apply(period: QuickAdvPeriod = QuickAdvPeriods.default,
            dateStart: LocalDate = LocalDate.now()): MDatesPeriod = {
    apply(
      period    = Some(period),
      dateStart = dateStart,
      dateEnd   = dateStart.plus( period.toPeriod.minusDays(1) )
    )
  }

}


case class MDatesPeriod(
  period                  : Option[QuickAdvPeriod],
  override val dateStart  : LocalDate,
  override val dateEnd    : LocalDate
)
  extends IDateStartEnd
