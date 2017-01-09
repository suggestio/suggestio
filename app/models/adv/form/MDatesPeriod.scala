package models.adv.form

import io.suggest.dt.interval.{QuickAdvIsoPeriod, QuickAdvPeriods}
import models.mdt.MDateStartEndT
import org.joda.time.{LocalDate, Period}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 16:06
 * Description: Модель с описанием периода размещения на основе двух дат либо периода-презета.
 */

object MDatesPeriod {

  def apply(period: QuickAdvIsoPeriod = QuickAdvPeriods.default,
            dateStart: LocalDate = LocalDate.now()): MDatesPeriod = {
    apply(
      quickPeriod     = Some(period),
      dateStart       = dateStart,
      dateEnd         = dateStart.plus {
        new Period(period.isoPeriod)
          .minusDays(1)
      }
    )
  }

}


case class MDatesPeriod(
  quickPeriod             : Option[QuickAdvIsoPeriod],
  override val dateStart  : LocalDate,
  override val dateEnd    : LocalDate
)
  extends MDateStartEndT
