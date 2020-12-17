package io.suggest.sjs.dt.period.m

import io.suggest.dt.interval.QuickAdvPeriod
import io.suggest.spa.DAction

import java.time.LocalDate

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 12:51
  * Description: Diode-экшены для date period.
  */
sealed trait DtpAction extends DAction

/** Действие обновление какой-то даты. */
case class SetDateStartEnd(fn: DtpInputFn, date: LocalDate) extends DtpAction

/** Действие выставления нового значения QuickAdvPeriod. */
case class SetQap(qap: QuickAdvPeriod) extends DtpAction
