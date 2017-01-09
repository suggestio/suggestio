package io.suggest.sc.sjs.c.plat

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.mdev.MPlatFsmSd
import io.suggest.sjs.common.fsm.{IFsmMsg, SjsFsm}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.10.16 11:49
  * Description: stub-trait для реализации кусков [[PlatformFsm]].
  */
trait PlatformFsmStub extends SjsFsm with StateData {

  override type State_t = FsmState
  override type SD      = MPlatFsmSd


  /** Уведомить всех ожидающих указанного события. */
  protected[this] def _broadcastToAll(event: String, signal: IFsmMsg): Unit = {
    for {
      listeners <- _stateData.subscribers.get( event )
    } {
      _broadcastToAll(listeners, signal)
    }
  }
  protected[this] def _broadcastToAll(listeners: TraversableOnce[SjsFsm], signal: IFsmMsg): Unit = {
    for (l <- listeners) {
      l ! signal
    }
  }

}
