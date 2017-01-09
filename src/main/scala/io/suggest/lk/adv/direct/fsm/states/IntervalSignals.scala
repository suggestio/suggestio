package io.suggest.lk.adv.direct.fsm.states

import io.suggest.lk.adv.direct.fsm.FsmStubT
import io.suggest.sjs.dt.period.fsm.IntervalSignalsBase
import io.suggest.sjs.interval.m.PeriodEith_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.12.15 16:05
 * Description: Трейты для сборки состояний, воспринимающих изменения периода размещения.
 */
trait IntervalSignals extends FsmStubT with IntervalSignalsBase {

  override protected[this] def _sdGetPeriod(sd: SD): PeriodEith_t = {
    sd.period
  }

  override protected[this] def _sdSetPeriod(newPeriod: PeriodEith_t, sd0: SD): SD = {
    sd0.copy(
      period = newPeriod
    )
  }

  protected trait PeriodSignalsStateT extends super.PeriodSignalsStateT {

    /** Состояние запроса к серверу за инфой (цена, отчет по датам размещения, etc). */
    override protected def _periodChanged(): Unit = {
      _upStart()
    }
  }

}
