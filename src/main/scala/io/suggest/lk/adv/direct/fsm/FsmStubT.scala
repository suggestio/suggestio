package io.suggest.lk.adv.direct.fsm

import io.suggest.fsm.StateData
import io.suggest.lk.adv.direct.m.MStateData
import io.suggest.sjs.common.fsm.SjsFsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 16:24
 * Description: Заготовка для FSM-аддонов формы прямого размещения.
 */
trait FsmStubT
 extends SjsFsm
 with StateData
{

  override type State_t = FsmState
  override type SD = MStateData

}
