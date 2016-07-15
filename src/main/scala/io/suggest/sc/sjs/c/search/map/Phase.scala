package io.suggest.sc.sjs.c.search.map

import io.suggest.sc.sjs.c.search.SearchFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.07.16 17:18
  * Description:
  */
trait Phase
  extends AwaitJs
  with JsInit
  with MapInit
  with Ready
  with Drag
{
  this: SearchFsm.type =>


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
