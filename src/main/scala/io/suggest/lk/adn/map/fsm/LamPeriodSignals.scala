package io.suggest.lk.adn.map.fsm

import io.suggest.sjs.dt.period.fsm.IntervalSignalsBase
import io.suggest.sjs.interval.m.PeriodEith_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 21:47
  * Description:
  */
trait LamPeriodSignals
  extends LamFsmStub
  with IntervalSignalsBase
{

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
