package io.suggest.ble.beaconer.fsm

import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.ble.beaconer.m.signals.Subscribe
import io.suggest.sjs.common.msg.ErrorMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:19
  * Description: FSM-аддон для пребывании в состоянии сна.
  */
trait Off extends BeaconerFsmStub {


  /** Трейт состояния ожидания подписок на мониторинг маячков. */
  trait OffStateT extends FsmState with IOnlineState {

    /** Реакция на запрос подписки на мониторинг маячков. */
    override def _handleSubscribe(s: Subscribe): Unit = {
      if (IBleBeaconsApi.detectApi.nonEmpty) {
        super._handleSubscribe(s)
        become(_onlineState)
      } else {
        error( ErrorMsgs.BLE_BEACONS_API_UNAVAILABLE )
      }
    }

  }

}
