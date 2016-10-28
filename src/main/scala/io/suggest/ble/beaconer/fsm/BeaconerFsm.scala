package io.suggest.ble.beaconer.fsm

import io.suggest.ble.api.IBleBeaconsApi
import io.suggest.ble.beaconer.m.MBeaconerFsmSd
import io.suggest.primo.IStart0
import io.suggest.sjs.common.fsm.SjsFsmImpl
import io.suggest.sjs.common.util.SjsLogger

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 12:18
  * Description: Реализация FSM мониторинга BLE-маячков.
  */
object BeaconerFsm {

  def applyIfPossible: Option[BeaconerFsm] = {
    for (_ <- IBleBeaconsApi.detectApi) yield {
      new BeaconerFsm
    }
  }

}


/** FSM мониторинга BLE-маячков. */
class BeaconerFsm
  extends SjsFsmImpl
  with Off
  with On
  with SjsLogger
  with IStart0
{

  override protected var _stateData: SD   = MBeaconerFsmSd()
  override protected var _state: State_t  = new DummyState

  private class DummyState extends FsmState

  /** Перевод FSM маячков в рабочий режим. */
  override def start(): Unit = {
    become( new OffS )
    // Подпиской на события видимости занимается ScFsm или кто-нибудь ещё, нас этот тут не волнует.
  }


  // State

  /** Пребывание в ожидании подписчика на сигналы. */
  class OffS extends OffStateT {
    override def _onlineState = new EarlyActiveS
  }


  sealed trait IOfflineState extends super.IOfflineState {
    override def _offlineState = new OffS
  }

  /** Ранняя активность системы. */
  class EarlyActiveS extends EarlyActiveStateT with IOfflineState {
    override def _activeState  = new ActiveS
  }

  /** Нормальная активность системы. */
  class ActiveS extends ActiveStateT with IOfflineState

}
