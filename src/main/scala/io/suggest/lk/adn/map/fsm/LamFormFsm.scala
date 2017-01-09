package io.suggest.lk.adn.map.fsm
import io.suggest.lk.adn.map.m.MLamFsmSd
import io.suggest.lk.adv.fsm.AdvFormChangedReceiver
import io.suggest.sjs.dt.period.vm.Container

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 18:53
  * Description: FSM обслуживания формы размещения на карте.
  */
class LamFormFsm
  extends LamUpPriceData
  with LamPeriodSignals
  with AdvFormChangedReceiver
{

  override protected var _state: State_t = new DummyState

  override protected var _stateData: SD = {
    val sdOpt = for {
      cont    <- Container.find()
      period  <- cont.getCurrPeriod
    } yield {
      MLamFsmSd(
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
    extends GetPriceStateT
    with PeriodSignalsStateT
    with AdvFormChangedReceiverStateT

}
