package io.suggest.sc.sjs.c.search.map

import io.suggest.primo.IStart0
import io.suggest.sc.sjs.c.gloc.GeoLocFsm
import io.suggest.sc.sjs.c.search.ITabFsmFactory
import io.suggest.sc.sjs.m.mgeo.{Subscribe, SubscriberData}
import io.suggest.sc.sjs.m.mmap.MMapSd
import io.suggest.sc.sjs.util.logs.ScSjsFsmLogger
import io.suggest.sjs.common.fsm.SjsFsmImpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 12:53
  * Description: FSM карты. Код изначально так и жил, потом внезапно был замёржен в SearchFsm, но затем
  * вынесен оттуда куда по-дальше.
  */
object MapFsm extends ITabFsmFactory {
  override type T = MapFsm
}

case class MapFsm()
  extends SjsFsmImpl
  with IStart0
  with AwaitJs
  with JsInit
  with MapInit
  with Ready
  with Drag
  with ScSjsFsmLogger
{

  override protected var _stateData: SD  = MMapSd()
  override protected var _state: State_t = new DummyState

  private class DummyState extends FsmState with FsmEmptyReceiverState


  /** Запуск этого FSM на исполнение. */
  override def start(): Unit = {
    become(new MapAwaitJsState)

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
  class MapAwaitJsState extends MapAwaitJsStateT {
    override def _jsReadyState = new MapJsInitState
  }

  /** Реализованный трейт WaitEnsureSignalT. */
  protected trait MapWaitEnsureT extends super.MapWaitEnsureT {
    override def _mapInitState = new MapInitState
  }
  /** Статическая js-поддержка карты инициализирована. */
  class MapJsInitState extends MapJsInitStateT with MapWaitEnsureT


  /** Динамическая часть карты в процессе инициализации. */
  class MapInitState extends MapInitStateT {
    override def _mapReadyState = new MapReadyState
  }

  /** Карта инициализирована. */
  class MapReadyState extends MapReadyStateT {
    override def _mapMovingState  = new MapDragState
    override def _mapInitState    = new MapInitState
  }

  /** Юзер таскает карту. */
  class MapDragState extends MapDragStateT {
    // TODO Нужно состояние подтверждения переключения в новую локацию?
    override def moveEndState = new MapReadyState
  }

}
