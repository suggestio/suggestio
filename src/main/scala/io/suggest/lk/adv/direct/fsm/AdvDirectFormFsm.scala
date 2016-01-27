package io.suggest.lk.adv.direct.fsm

import io.suggest.lk.adv.direct.fsm.states.StandBy
import io.suggest.lk.adv.direct.m.MStateData
import io.suggest.lk.dt.interval.vm.Container
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 16:22
 * Description: Конечный автомат, обслуживающий нетривиальную форму прямого размещения на узлах.
 */

object AdvDirectFormFsm
  extends StandBy
  with SjsLogger
{

  /** Инстансы stateData храняться здесь.
    * Здесь же происходит начальная инициализация. */
  override protected var _stateData: SD = {
    val sdOpt = for {
      cont    <- Container.find()
      period  <- cont.getCurrPeriod
    } yield {
      MStateData(
        period = period
      )
    }
    sdOpt.get
  }

  private class DummyState extends FsmEmptyReceiverState

  /** Начальное состояние FSM. */
  override protected var _state: State_t = new DummyState


  /** Запуск этого FSM. */
  def start(): Unit = {
    become(new StandByState)
  }


  /*-------------- states -------------------*/

  class StandByState extends StandByStateT {
    // TODO Обновлять состояние
    override protected def _srvUpdateFormState: FsmState = _updatePriceState
  }

  /** Инстанс состояния обновления цены. */
  override protected def _updatePriceState: FsmState = _state // TODO

}
