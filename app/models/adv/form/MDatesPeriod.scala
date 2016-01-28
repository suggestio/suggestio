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

  def apply(): MDatesPeriod = {
    apply(
      period    = QuickAdvPeriods.default,
      dateStart = LocalDate.now().plusDays(1)
    )
  }

  def apply(period: QuickAdvPeriod, dateStart: LocalDate): MDatesPeriod = {
    apply(
      period    = Some(period),
      dateStart = dateStart,
      dateEnd   = dateStart.plus(period.toPeriod)
    )
  }

}


case class MDatesPeriod(
  period                  : Option[QuickAdvPeriod],
  override val dateStart  : LocalDate,
  override val dateEnd    : LocalDate
)
  extends IDateStartEnd
