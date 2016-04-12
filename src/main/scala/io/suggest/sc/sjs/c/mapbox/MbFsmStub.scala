package io.suggest.sc.sjs.c.mapbox

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.mmap.MbFsmSd
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 19:12
  * Description: Черновик для сборки кусков и состояний MapBox FSM.
  */
trait MbFsmStub extends SjsFsm with StateData {
  override type State_t = FsmState
  override type SD = MbFsmSd
}
