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
  with JsInit
  with MapInit
  with MapReady
  with OnMove
  with Detach
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
    override def _jsReadyState = new JsInitState
  }

  // TODO объеденить MapInitializing.afterBecome и одну из синхронных реализаций _handleEnsureMap().

  /** Реализованный трейт WaitEnsureSignalT. */
  protected trait EnsureMapInitT extends super.EnsureMapInitT {
    override def _mapInitializingState = new MapInitState
  }
  /** Статическая js-поддержка карты инициализирована. */
  class JsInitState extends JsInitStateT with EnsureMapInitT

  /** Состояние, когда нет инициализированной карты. */
  class NoMapState extends EnsureMapInitT


  /** Динамическая часть карты в процессе инициализации. */
  class MapInitState extends MapInitStateT {
    override def _mapReadyState = new MapReadyState
  }

  /** Карта инициализирована. */
  class MapReadyState extends MapReadyStateT with HandleScInxSwitch {
    override def _mapMovingState  = new OnDragState
    override def _detachedState   = new MapDetachedState
    override def _noMapState      = new NoMapState
  }

  /** Юзер таскает карту. */
  class OnDragState extends OnDragStateT {
    // TODO Нужно состояние подтверждения переключения в новую локацию?
    override def moveEndState = new MapReadyState
  }

  /** Состояние отсоединенности карты от выдачи. */
  class MapDetachedState extends MapDetachedStateT {
    override def _detachReattachFailedState = new MapInitState
    override def _attachSuccessState        = new MapReadyState
  }

}
