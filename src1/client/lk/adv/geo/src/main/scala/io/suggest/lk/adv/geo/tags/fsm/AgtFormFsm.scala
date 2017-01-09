package io.suggest.lk.adv.geo.tags.fsm

import io.suggest.lk.adv.fsm.AdvFormChangedReceiver
import io.suggest.lk.adv.geo.tags.fsm.states.Rcvrs
import io.suggest.lk.adv.geo.tags.m.MAgtStateData
import io.suggest.sjs.dt.period.vm.Container

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 12:17
  * Description: FSM, обслуживающий страницу/форму размещения карточки в гео-тегах.
  */
object AgtFormFsm
  extends states.UpdatePriceData
  with states.PeriodSignals
  with AdvFormChangedReceiver
  with Rcvrs
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
    extends AdvFormChangedReceiverStateT
    with GetPriceStateT
    with PeriodSignalsStateT
    with RcvrsStateT

}
