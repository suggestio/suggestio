package io.suggest.sc.sjs.c.plat

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.mdev.MPlatFsmSd
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 11:49
  * Description: stub-trait для реализации кусков [[PlatformFsm]].
  */
trait PlatformFsmStub extends SjsFsm with StateData {

  override type State_t = FsmState
  override type SD      = MPlatFsmSd

}
