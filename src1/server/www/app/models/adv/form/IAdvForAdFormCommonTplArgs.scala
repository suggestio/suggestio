package models.adv.form

import io.suggest.dt.interval.QuickAdvPeriods

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 14:42
  * Description:
  */
trait IAdvForAdFormCommonTplArgs {

  /** Доступные для рендера периоды. */
  def advPeriodsAvail: Seq[String] = {
    QuickAdvPeriods.values
      .map(_.value)
  }

}
