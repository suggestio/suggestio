package io.suggest.ble.beaconer.fsm

import io.suggest.ble.beaconer.m.MBeaconerFsmSd
import io.suggest.ble.beaconer.m.signals.{Subscribe, UnSubscribe}
import io.suggest.fsm.StateData
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:00
  * Description: fsm stub для сборки трейтов-аддонов и с состояниями FSM.
  */
trait BeaconerFsmStub extends SjsFsm with StateData {

  trait FsmState extends super.FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart.orElse {
      // Сигнал первой подписки на мониторинг маячков.
      case s: Subscribe =>
        _handleSubscribe(s)
      case u: UnSubscribe =>
        _handleUnSubscribe(u)
    }

    /** Реакция на запрос подписки на мониторинг маячков. */
    def _handleSubscribe(s: Subscribe): Unit = {
      val sd0 = _stateData
      val sd1 = sd0.withWatchers(
        s.fsm :: sd0.watchers
      )
      _stateData = sd1
    }

    /** Реация на unsubscribe-сигнал. */
    def _handleUnSubscribe(u: UnSubscribe): Unit = {
      val sd0 = _stateData
      if (sd0.watchers.nonEmpty) {
        _stateData = sd0.withWatchers(
          sd0.watchers.filter(_ != u.fsm)
        )
      }
    }

  }

  override type State_t = FsmState

  override type SD = MBeaconerFsmSd

}
