package io.suggest.sc.sjs.c.mapbox

import io.suggest.sc.sjs.c.gloc.GeoLocFsm
import io.suggest.sc.sjs.m.mgeo.{Subscribe, SubscriberData}
import io.suggest.sc.sjs.m.mmap.MbFsmSd
import io.suggest.sc.sjs.util.logs.ScSjsFsmLogger
import io.suggest.sjs.common.fsm.SjsFsmImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 18:37
  * Description: MapBox FSM: система обслуживания карты, начиная с фоновой инициализации.
  *
  * Весь процесс асинхронен и независим от ScFsm, кроме финального уведомления ScFsm об итогах инициализации.
  * Скрипт mapbox грузиться асинхронно, поэтому его надо поджидать с использованием таймера.
  */
object MbFsm
  extends SjsFsmImpl
  with AwaitMbglJs
  with JsInitializing
  with MapInitializing
  with MapReady
  with OnMove
  with ScSjsFsmLogger
  //with LogBecome
{

  override protected var _stateData: SD = MbFsmSd()
  override protected var _state: State_t = new DummyState

  private class DummyState extends FsmState with FsmEmptyReceiverState


  /** Запуск этого FSM на исполнение. */
  def start(): Unit = {
    become(new AwaitMbglJsState)

    // Подписаться на события геолокации текущего юзера.
    GeoLocFsm ! Subscribe(
      receiver    = this,
      notifyZero  = true,
      data        = SubscriberData(
        withErrors = false
      )
    )
  }


  // -- states impl
  /** Состояние ожидания и начальной инициализации карты. */
  class AwaitMbglJsState extends AwaitMbglJsStateT {
    override def jsReadyState = new JsInitializingState
  }

  /** Статическая js-поддержка карты инициализирована. */
  class JsInitializingState extends JsInitializingStateT {
    override def _mapInitializingState = new MapInitializingState
  }

  /** Динамическая часть карты в процессе инициализации. */
  class MapInitializingState extends MapInitializingStateT {
    override def mapReadyState = new MapReadyState
  }

  /** Карта инициализирована. */
  class MapReadyState extends MapReadyStateT {
    override def mapMovingState = new OnDragState
  }

  /** Юзер таскает карту. */
  class OnDragState extends OnDragStateT {
    // TODO Нужно состояние подтверждения переключения в новую локацию?
    override def moveEndState = new MapReadyState
  }

}
