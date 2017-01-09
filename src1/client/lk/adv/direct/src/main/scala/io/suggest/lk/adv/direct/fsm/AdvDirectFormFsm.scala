package io.suggest.lk.adv.direct.fsm

import io.suggest.lk.adv.direct.fsm.states.{GetData, StandBy}
import io.suggest.lk.adv.direct.m.MStateData
import io.suggest.sjs.dt.period.vm.Container

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 16:22
 * Description: Конечный автомат, обслуживающий нетривиальную форму прямого размещения на узлах.
 */

object AdvDirectFormFsm
  extends StandBy
  with GetData
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

  class StandByState
    extends StandByStateT
    with GetPriceStateT

}
