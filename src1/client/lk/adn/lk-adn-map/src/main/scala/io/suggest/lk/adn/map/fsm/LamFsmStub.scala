package io.suggest.lk.adn.map.fsm

import io.suggest.fsm.StateData
import io.suggest.lk.adn.map.m.MLamFsmSd
import io.suggest.lk.adv.fsm.IUpdatePriceDataStart
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.16 18:55
  * Description: fsm stub для сборки трейтов-кусков FSM формы.
  */
trait LamFsmStub
  extends SjsFsm
  with StateData
  with IUpdatePriceDataStart
{

  override type State_t = FsmState
  override type SD      = MLamFsmSd

}
