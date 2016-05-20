package io.suggest.sc.sjs.c.gloc

import io.suggest.sc.sjs.m.magent.VisibilityChange
import io.suggest.sc.sjs.m.mgeo.MGeoFsmSd
import io.suggest.sc.sjs.util.logs.ScSjsLogger
import io.suggest.sc.sjs.vm.SafeDoc
import io.suggest.sjs.common.fsm.SjsFsmImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.04.16 10:30
  * Description: Реализация FSM, взаимодействующего с API геолокации браузера и другими FSM.
  *
  * Зачем?
  * 1. Потому что в браузере есть два вида геолокации, а разбираться в них клиенты этого FSM не обязаны.
  *    Им всем просто нужна геолокация, и чтобы как можно точнее.
  * 2. Нужно засыпать и просыпаться, чтобы не жрать ресурсы неактивного мобильного устройства.
  * 3. FSM может отрабатывать возможные косяки взаимодействия внешнего браузера с системой выдачи.
  * 4. Методы геолокации должны включаться и отключаться на ходу.
  */
object GeoLocFsm
  extends SjsFsmImpl
    with ScSjsLogger
    with Off
    with Watching
    //with LogBecome
{

  override protected var _stateData: SD   = MGeoFsmSd()
  override protected var _state: State_t  = new DummyState

  /** Запуск данного FSM. Вызывается только один раз. */
  def start(): Unit = {
    become(new OffState)
    // Подписаться FSM на события изменения видимости текущей вкладки.
    SafeDoc.addEventListener("visibilitychange")( _signalCallbackF(VisibilityChange) )
  }

  /** Затычка начального состояния. */
  private class DummyState extends FsmState with FsmEmptyReceiverState

  /** Состояние отключённости от системы. */
  class OffState extends OffStateT {
    override def _watchingState = new WatchingState
  }

  /** Ожидание просыпания страницы/девайса. */
  class SleepingState extends SleepingStateT {
    override def _watchingState = new WatchingState
  }

  /** Состояние наблюдения за данными геолокации. */
  class WatchingState extends WatchingStateT {
    override def _sleepingState = new SleepingState
    override def _offState = new OffState
  }

}
