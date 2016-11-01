package io.suggest.sc.sjs.c.gloc

import io.suggest.sc.sjs.c.plat.PlatformFsm
import io.suggest.sc.sjs.m.mdev.{PlatEventListen, PlatformEvents}
import io.suggest.sc.sjs.m.mgeo.MGeoFsmSd
import io.suggest.sc.sjs.util.logs.ScSjsFsmLogger
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
  with ScSjsFsmLogger
  with Off
  with Watching
  //with LogBecome
{

  override protected var _stateData: SD   = MGeoFsmSd()
  override protected var _state: State_t  = new DummyState


  /** Дополнительно можно логгировать какое-то "состояние". */
  override protected def _logState: Option[String] = {
    Some( _state.toString + "(" + _stateData + ")" )
  }

  /** Запуск данного FSM. Вызывается только один раз. */
  def start(): Unit = {
    become(new OffState)

    // Подписаться FSM на события изменения видимости текущего view'а.
    PlatformFsm ! PlatEventListen( PlatformEvents.VISIBILITY_CHANGE, this, subscribe = true )
  }

  /** Затычка начального состояния. */
  private class DummyState extends FsmState with FsmEmptyReceiverState


  protected trait IWatchingState extends super.IWatchingState {
    override def _watchingState = new WatchingState
  }

  /** Состояние отключённости от системы. */
  class OffState
    extends OffStateT
    with IWatchingState

  /** Ожидание просыпания страницы/девайса. */
  class SleepingState
    extends SleepingStateT
    with IWatchingState

  /** Состояние наблюдения за данными геолокации. */
  class WatchingState extends WatchingStateT {
    override def _sleepingState = new SleepingState
    override def _offState = new OffState
  }

}
