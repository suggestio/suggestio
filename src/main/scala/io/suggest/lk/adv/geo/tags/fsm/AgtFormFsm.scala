package io.suggest.lk.adv.geo.tags.fsm

import io.suggest.lk.adv.geo.tags.m.MAgtStateData
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.sjs.dt.period.vm.Container

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 12:17
  * Description: FSM, обслуживающий страницу/форму размещения карточки в гео-тегах.
  */
class AgtFormFsm
  extends states.StandBy
  with states.UpdatePriceData
  with states.PeriodSignals
  with SjsLogger
{

  override type State_t = FsmState
  override protected var _state: State_t = new DummyState

  override protected var _stateData: SD = {
    val sdOpt = for {
      cont    <- Container.find()
      period  <- cont.getCurrPeriod
    } yield {
      MAgtStateData(
        period = period
      )
    }
    sdOpt.get
  }

  private class DummyState extends FsmEmptyReceiverState


  def start(): Unit = {
    become(new StandByState)
  }


  // -- states --

  class StandByState
    extends StandByStateT
    with GetPriceStateT
    with PeriodSignalsStateT

}
