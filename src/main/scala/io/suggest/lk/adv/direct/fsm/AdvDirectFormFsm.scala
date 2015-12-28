package io.suggest.lk.adv.direct.fsm

import io.suggest.lk.adv.direct.m.MStateData
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 16:22
 * Description: Конечный автомат, обслуживающий нетривиальную форму прямого размещения на узлах.
 */

object AdvDirectFormFsm
  extends FsmStubT
  with SjsLogger
{

  override protected var _stateData: SD = MStateData()

  /** Начальное состояние FSM. */
  override protected var _state: State_t = ???

}
