package io.suggest.sc.sjs.c.scfsm.init

import io.suggest.sc.sjs.c.scfsm.geo.{GeoInit, IndexPreload, Timeout}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.09.15 10:45
 * Description: Трейт фазы стартовой геолокации. Здесь собраны состояния,
 * которые необходимы для начальной геолокации при запуске выдачи.
 */
trait Phase extends GeoInit with IndexPreload with Timeout with JsRouterInit with Init {

  /** Таймаут геолокации при запуске. */
  private def INIT_GEO_TIMEOUT_MS = 5500

  /** Вход в фазу начальной инициализации.*/
  protected def _initPhaseEnter1st: FsmState = new FirstInitState

  /** Выход из фазы начальной инициализации и геолокации. */
  protected def _initPhaseExit_OnWelcomeGridWait_State: FsmState
  protected def _initPhaseExit_OnGridWait_State: FsmState


  /** Частичная реализация GeoWaitStateT для нужд инициализации. */
  protected trait GeoWaitAnyStateT extends GeoWaitStateT {
    protected def _geoFinishedState: FsmState

    override protected def _geoFailedState = _geoFinishedState
    override protected def _geoReadyState  = _geoFinishedState
  }

  /** Частичная реализация для NormalInitStateT в целях дедубликации кода. */
  protected trait NormalInitStateT extends super.NormalInitStateT {
    override protected def _geoAskState = new Init_JsRouterWait_GeoAskWait_TimeoutAskWait_State
  }

  /** Частичная реализация ProcessIndexReceivedUtil в целях дедубликации кода. */
  protected trait ProcessIndexReceivedUtil extends super.ProcessIndexReceivedUtil {
    override protected def _welcomeAndWaitGridAdsState  = _initPhaseExit_OnWelcomeGridWait_State
    override protected def _waitGridAdsState            = _initPhaseExit_OnGridWait_State
  }

  /** Реализация IGeoTimeout для дедубликации кода. */
  protected trait IGeoTimeout extends super.IGeoTimeout {
    override def _geoTimeoutNeedIndexState = new Init_InxWait_State
  }


  /** Реализация состояния самой первой инициализации. */
  class FirstInitState
    extends FsmEmptyReceiverState
    with JsRouterInitStartStateT
    with NormalInitStateT
    with FirstInitStateT

  /** Реализация состояния типовой инициализации. */
  class NormalInitState
    extends FsmEmptyReceiverState
    with NormalInitStateT


  /** Одноразовое состояние, когда параллельные инициализации запущены: jsrouter, геолокация, geo timer.
    * Тут же происходит запуск geo ask и geo timer. */
  class Init_JsRouterWait_GeoAskWait_TimeoutAskWait_State
    extends JsRouterInitReceiveT
    with BssGeoAskStartT with GeoWaitAnyStateT
    with InstallGeoTimeoutStateT with ListenGeoTimerStateT
    with ProcessIndexReceivedUtil
    with IGeoTimeout
  {
    // js router wait
    override def _jsRouterReadyState          = new Init_GeoWait_InxAskWait_TimeoutWait_State
    override protected def _reInitState       = new NormalInitState
    // geo ask
    override def GEO_TIMEOUT_MS               = INIT_GEO_TIMEOUT_MS
    // geo wait. Остаёмся на текущем состоянии, ожидая jsrouter.
    override protected def _geoFinishedState  = null
  }


  /** Запуск geo index ask, когда инициализация jsRouter'а уже выполнена, а геолокация ещё в процессе. */
  class Init_GeoWait_InxAskWait_TimeoutWait_State
    extends FsmEmptyReceiverState
    with GeoWaitAnyStateT
    with AskGeoIndex with GeoIndexWaitStateT
    with ListenGeoTimerStateT
    with ProcessIndexReceivedUtil
    with IGeoTimeout
  {
    override protected def _geoFinishedState  = new Init_GeoWait_InxAskWait_TimeoutWait_State
    override def _getNodeIndexFailedState     = new Init_InxWait_State
    override def _waitMoreGeoState            = null
  }


  /** Состояние ожидания уже запрошенного индекса выдачи без дополнительных проверок. */
  // TODO Таймаут ожидания, если в sd уже есть полученный закешированный index.
  class Init_InxWait_State
    extends GeoIndexWaitSimpleStateT
    with ProcessIndexReceivedUtil
  {
    override def _getNodeIndexFailedState = new NormalInitState
  }

}
