package io.suggest.lk.adv.geo.tags.fsm.states

import io.suggest.lk.adv.geo.tags.fsm.AgtFormFsmStub
import io.suggest.sjs.dt.period.fsm.IntervalSignalsBase
import io.suggest.sjs.interval.m.PeriodEith_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.16 13:04
  * Description: Поддержка реакции на изменения в виджете выбора диапазона дат размещения.
  */
trait PeriodSignals extends AgtFormFsmStub with IntervalSignalsBase {

  override protected[this] def _sdGetPeriod(sd: SD): PeriodEith_t = {
    sd.period
  }

  override protected[this] def _sdSetPeriod(newPeriod: PeriodEith_t, sd0: SD): SD = {
    sd0.copy(
      period = newPeriod
    )
  }

  protected trait PeriodSignalsStateT extends super.PeriodSignalsStateT {
    override protected def _periodChanged(): Unit = {
      _upStart()
    }
  }

}
