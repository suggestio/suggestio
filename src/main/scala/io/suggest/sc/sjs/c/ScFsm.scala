package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.scfsm._
import io.suggest.sc.sjs.c.scfsm.init.Init
import io.suggest.sc.sjs.c.scfsm.node.Index
import io.suggest.sc.sjs.m.msc.fsm.{IStData, MStData}
import io.suggest.sc.sjs.m.msearch.MTabs
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: FSM-контроллер для всей выдачи. Собирается из кусков, которые закрывают ту или иную область.
 */
object ScFsm extends SjsLogger with Init with Index with GridAppend with OnPlainGrid with OnGridSearchGeo
with node.States with OnGridSearchHashTags with OnGridNav with foc.StartingForAd with foc.OnFocus with foc.Closing
with foc.SimpleShift with foc.PreLoading with foc.OnTouch with foc.OnTouchPreload {

  // Инициализируем базовые внутренние переменные.
  override protected var _state: FsmState = new DummyState

  /** Контейнер данных состояния. */
  override protected var _stateData: SD = MStData()

  /** Текущий обработчик входящих событий. */
  private var _receiver: Receive = _

  /** Выставление указанного ресивера в качестве обработчика событий. */
  override protected def _installReceiver(newReceiver: Receive): Unit = {
    _receiver = newReceiver
  }


  /** Ресивер для всех состояний. */
  override protected val allStatesReceiver = super.allStatesReceiver

  // Обработать событие синхронно.
  override protected def _sendEventSync(e: Any): Unit = {
    _receiver(e)
  }

  def firstStart(): Unit = {
    become(new FirstInitState)
  }


  // Далее идёт реализации FSM-состояний. Внутреняя логика состояний раскидана по аддонам.

  /*--------------------------------------------------------------------------------
   * Состояния ранней инициализации выдачи.
   *--------------------------------------------------------------------------------*/

  protected trait NormalInitStateT extends super.NormalInitStateT {
    override protected def _geoAskState = new Init_JsRouterWait_GeoAskWait_State
  }

  /** Реализация состояния самой первой инициализации. */
  class FirstInitState extends FirstInitStateT with NormalInitStateT
  /** Реализация состояния типовой инициализации. */
  class NormalInitState extends NormalInitStateT

  // init-геолокация и инициализация jsRouter'а.
  /** Две параллельные инициализации запущены: jsrouter и геолокация. */
  class Init_JsRouterWait_GeoAskWait_State extends Init_JsRouterWait_GeoAskWait_StateT {
    override def _jsRouterReadyState = new Init_GeoWait_State
    override protected def _reInitState = new NormalInitState
    override protected def _geoFinishedState = new Init_JsRouterWaitState
  }
  /** Инициализация геолокации, когда инициализация jsRouter'а уже выполнена. */
  class Init_GeoWait_State extends Init_GeoWait_StateT {
    override protected def _geoFinishedState = new NodeInit_GetIndex_WaitIndex_State
  }
  /** Маловероятное состояние, когда инициализация геолокации прошла быстрее, чем инициализация jsRouter'а. */
  class Init_JsRouterWaitState extends JsRouterInitReceiveT {
    override def _jsRouterReadyState = new NodeInit_GetIndex_WaitIndex_State
    override protected def _reInitState = new NormalInitState
  }


  /*--------------------------------------------------------------------------------
   * Фаза состояний инициализации выдачи конкретного узла (node index).
   *--------------------------------------------------------------------------------*/
  // TODO может перекинуть классы-реализации внутрь node.States + добавить метод для выхода из фазы на OnPlainGrid?

  /** Реализация состояния-получения-обработки индексной страницы. */
  class NodeInit_GetIndex_WaitIndex_State extends NodeInit_GetIndex_WaitIndex_StateT {
    override protected def _welcomeAndWaitGridAdsState  = new NodeInit_WelcomeShowing_GridAdsWait_State
    override protected def _onNodeIndexFailedState      = this
    override protected def _waitGridAdsState            = new NodeInit_GridAdsWait_State
  }
  class NodeInit_WelcomeShowing_GridAdsWait_State extends NodeInit_WelcomeShowing_GridAdsWait_StateT {
    override protected def _welcomeFinishedState        = new NodeInit_GridAdsWait_State
    override protected def _adsLoadedState              = new NodeInit_WelcomeShowing_State
    override protected def _findAdsFailedState          = new NodeInit_WelcomeShowing_GridAdsFailed_State
    override protected def _welcomeHidingState          = new NodeInit_WelcomeHiding_State
  }
  class NodeInit_WelcomeShowing_State extends NodeInit_WelcomeShowing_StateT {
    override protected def _welcomeFinishedState        = new NodeInit_GridAdsWait_State
    override protected def _welcomeHidingState          = new NodeInit_WelcomeHiding_State
  }
  class NodeInit_WelcomeHiding_GridAdsWait_State extends NodeInit_WelcomeHiding_GridAdsWait_StateT {
    override protected def _welcomeFinishedState        = new NodeInit_GridAdsWait_State
    override protected def _findAdsFailedState          = new NodeInit_WelcomeHiding_GridAdsFailed_State
    override protected def _adsLoadedState              = new NodeInit_WelcomeHiding_State
  }
  class NodeInit_GridAdsWait_State extends NodeInit_GridAdsWait_StateT {
    override protected def _adsLoadedState              = new OnPlainGridState
    override protected def _findAdsFailedState          = new NodeInit_GridAdsFailed_State
  }
  class NodeInit_WelcomeHiding_State extends NodeInit_WelcomeHiding_StateT {
    override protected def _welcomeFinishedState        = new OnPlainGridState
  }
  class NodeInit_WelcomeShowing_GridAdsFailed_State extends NodeInit_WelcomeShowing_GridAdsFailed_StateT {
    override protected def _welcomeFinishedState        = new NodeInit_GridAdsWait_State
    override protected def _welcomeHidingState          = new NodeInit_WelcomeHiding_GridAdsWait_State
    override protected def _findAdsFailedState          = this
    override protected def _adsLoadedState              = new NodeInit_WelcomeShowing_State
  }
  class NodeInit_WelcomeHiding_GridAdsFailed_State extends NodeInit_WelcomeHiding_GridAdsFailed_StateT {
    override protected def _welcomeFinishedState        = new NodeInit_GridAdsWait_State
    override protected def _findAdsFailedState          = this
    override protected def _adsLoadedState              = new NodeInit_WelcomeHiding_State
  }
  class NodeInit_GridAdsFailed_State extends NodeInit_GridAdsFailed_StateT {
    override protected def _adsLoadedState              = new OnPlainGridState
    override protected def _findAdsFailedState          = this
  }


  /*--------------------------------------------------------------------------------
   * Фаза состояний работы с сеткой карточек (grid).
   *--------------------------------------------------------------------------------*/
  /** Трейт для поддержки переключения на состояния, исходящие из OnGridStateT  */
  protected trait OnGridStateT extends super.OnGridStateT {
    override def _startFocusOnAdState = new FocStartingForAd
  }

  /** Превратить search-таб в соответствующее состояние. */
  protected def _searchTab2state(sd1: IStData): FsmState = {
    sd1.search.currTab match {
      case MTabs.Geo      => new OnGridSearchGeoState
      case MTabs.HashTags => new OnGridSearchHashTagsState
    }
  }

  /** Реализация состояния, где карточки уже загружены. */
  class OnPlainGridState extends OnPlainGridStateT with OnGridStateT {
    override def _nextStateSearchPanelOpened(sd1: MStData) = _searchTab2state(sd1)
    override protected def _navLoadListState = new OnGridNavLoadListState
  }


  /*--------------------------------------------------------------------------------
   * Состояния нахождения в сетке и на поисковой панели одновременно (grid + search).
   *--------------------------------------------------------------------------------*/
  /** Общий код для реакции на закрытие search-панели. */
  protected[this] trait _SearchClose extends OnGridSearchStateT {
    override def _nextStateSearchPanelClosed = new OnPlainGridState
  }

  /** Состояние, где и сетка есть, и поисковая панель отрыта на вкладке географии. */
  class OnGridSearchGeoState extends OnGridSearchGeoStateT with _SearchClose with OnGridStateT {
    override def _tabSwitchedFsmState = new OnGridSearchHashTagsState
  }

  /** Состояние, где открыта вкладка хеш-тегов на панели поиска. */
  class OnGridSearchHashTagsState extends OnGridSearchHashTagsStateT with _SearchClose with OnGridStateT {
    override def _tabSwitchedFsmState = new OnGridSearchGeoState
  }


  /*--------------------------------------------------------------------------------
   * Состояния нахождения в сетке и на панели навигации одновременно.
   *--------------------------------------------------------------------------------*/
  /** Вспомогательный трейт для сборки  */
  protected trait _OnGridNav extends super._OnGridNav {
    override def _onHideNavState = new OnPlainGridState
  }
  /** Состояние отображения панели навигации с текущей подгрузкой списка карточек. */
  class OnGridNavLoadListState extends OnGridNavLoadListStateT with _OnGridNav with OnGridStateT {
    override def _navPanelReadyState = new OnGridNavReadyState
  }
  protected trait _NodeSwitchState extends INodeSwitchState {
    override def _onNodeSwitchState = new NodeInit_GetIndex_WaitIndex_State
  }
  class OnGridNavReadyState extends OnGridNavReadyStateT with _OnGridNav with _NodeSwitchState with OnGridStateT


  /*--------------------------------------------------------------------------------
   * Состояния focused-выдачи.
   *--------------------------------------------------------------------------------*/
  /** Состояние сразу после клика по карточке в плитке. Отрабатывается запрос, происходит подготовка focused-выдачи. */
  class FocStartingForAd extends StartingForAdStateT {
    override def _focOnAppearState = new FocAppearingState
    override def _backToGridState  = new OnPlainGridState
  }
  class FocAppearingState extends OnAppearStateT {
    override def _focReadyState = new FocOnFocusState
  }
  protected trait ISimpleShift extends super.ISimpleShift {
    override def _shiftRightState = new FocShiftRightState
    override def _shiftLeftState  = new FocShiftLeftState
  }
  protected trait OnFocusStateBaseT extends super.OnFocusStateBaseT with _NodeSwitchState with ISimpleShift {
    override def _closingState    = new FocClosingState
  }
  /** Состояние нахождения на какой-то focused-карточке в выдаче. */
  class FocOnFocusState extends OnFocusStateT with OnFocusStateBaseT {
    override def _leftPreLoadState  = new FocPreLoadLeftState
    override def _rightPreLoadState = new FocPreLoadRightState
    override def _onTouchStartState = new FocTouchStartState
  }
  /** Состояние закрытия focused-выдачи с возвратом в плитку. */
  class FocClosingState extends FocClosingStateT {
    override def _afterDisappearState: FsmState = {
      val sd0 = _stateData
      if (sd0.nav.panelOpened) {
        new OnGridNavReadyState
      } else if (sd0.search.opened) {
        _searchTab2state(sd0)
      } else {
        new OnPlainGridState
      }
    }
  }
  // Переключение focused-карточек в выдаче.
  protected trait SimpleShiftStateT extends super.SimpleShiftStateT {
    override def _shiftDoneState = new FocOnFocusState
  }
  /** Состояние перехода на одну карточку вправо. */
  class FocShiftRightState extends ShiftRightStateT with SimpleShiftStateT
  /** Состояние перехода на одну карточку влево. */
  class FocShiftLeftState  extends ShiftLeftStateT with SimpleShiftStateT

  /** Общий код реализаций focused-preload-состояний. */
  protected trait FocPreLoadingStateT extends super.FocPreLoadingStateT with OnFocusStateBaseT {
    override def _preloadDoneState = new FocOnFocusState
  }
  /** Состояние нахождения на крайней правой карточке среди уже подгруженных,
    * но НЕ крайней среди имеющихся в focused-выборке. */
  class FocPreLoadRightState extends FocPreLoadingStateT with FocRightPreLoadingStateT {
    override def _onTouchStartState = new FocTouchShiftPreloadRightState
  }
  class FocPreLoadLeftState  extends FocPreLoadingStateT with FocLeftPreLoadingStateT {
    override def _onTouchStartState = new FocTouchShiftPreloadLeftState
  }

  // Поддержка touch-состояний в focused-выдаче.
  protected trait FocTouchCancelledT extends super.FocTouchCancelledT {
    override def _touchCancelledState = new FocOnFocusState
  }
  class FocTouchStartState extends FocTouchCancelledT with FocOnTouchStartStateT {
    override def _touchShiftState   = new FocTouchShiftState
    override def _touchVScrollState = new FocTouchStartState
  }
  class FocTouchScrollState extends FocTouchCancelledT with FocOnTouchScrollStateT
  class FocTouchShiftState  extends FocOnTouchShiftStateT with ISimpleShift with FocTouchCancelledT

  // Touch-навигация началась одновременно с идущим preload. Тут состояния для поддержки подобного явления.
  protected trait FocPreLoadingReceiveStateT extends super.FocPreLoadingReceiveStateT {
    override def _preloadDoneState = new FocTouchShiftState
  }
  class FocTouchShiftPreloadLeftState
    extends OnTouchShiftPreloadLeftStateT
    with ISimpleShift
    with FocTouchCancelledT
    with FocPreLoadingReceiveStateT
  class FocTouchShiftPreloadRightState
    extends OnTouchShiftPreloadRightStateT
    with ISimpleShift
    with FocTouchCancelledT
    with FocPreLoadingReceiveStateT

}
