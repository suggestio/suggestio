package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.m.msc.fsm.{IStData, MStData}
import io.suggest.sc.sjs.m.msearch.MTabs
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: FSM-контроллер для всей выдачи. Собирается из кусков, которые закрывают ту или иную область.
 */
object ScFsm
  extends SjsLogger
  with init.Phase
  with node.States
  with grid.Append with grid.Plain with grid.LoadMore
  with OnGridNav
  with search.OnGeo
  with search.tags.Opened
  with foc.Phase
{

  // Инициализируем базовые внутренние переменные.
  override protected var _state: FsmState = new DummyState

  /** Контейнер данных состояния. */
  override protected var _stateData: SD = MStData()


  /** Ресивер для всех состояний. */
  override protected val allStatesReceiver = super.allStatesReceiver

  def start(): Unit = {
    become( _initPhaseEnter1st )
  }


  // Далее идёт реализации FSM-состояний. Внутреняя логика состояний раскидана по аддонам.

  /*--------------------------------------------------------------------------------
   * Состояния ранней инициализации выдачи.
   *--------------------------------------------------------------------------------*/

  override protected def _initPhaseExit_OnWelcomeGridWait_State = {
    new NodeInit_WelcomeShowing_GridAdsWait_State
  }
  override protected def _initPhaseExit_OnGridWait_State = {
    new NodeInit_GridAdsWait_State
  }


  /*--------------------------------------------------------------------------------
   * Фаза состояний инициализации выдачи конкретного узла (node index).
   *--------------------------------------------------------------------------------*/
  // TODO может перекинуть классы-реализации внутрь node.States + добавить метод для выхода из фазы на OnPlainGrid?

  protected trait ProcessIndexReceivedUtil extends super.ProcessIndexReceivedUtil {
    override protected def _welcomeAndWaitGridAdsState  = new NodeInit_WelcomeShowing_GridAdsWait_State
    override protected def _waitGridAdsState            = new NodeInit_GridAdsWait_State
  }

  /** Реализация состояния-получения-обработки индексной страницы. */
  class NodeInit_GetIndex_WaitIndex_State extends NodeInit_GetIndex_WaitIndex_StateT with ProcessIndexReceivedUtil {
    override protected def _onNodeIndexFailedState      = this
  }
  class NodeInit_WelcomeShowing_GridAdsWait_State extends NodeInit_WelcomeShowing_GridAdsWait_StateT {
    override protected def _welcomeFinishedState        = new NodeInit_GridAdsWait_State
    override protected def _adsLoadedState              = new NodeInit_WelcomeShowing_State
    override protected def _findAdsFailedState          = new NodeInit_WelcomeShowing_GridAdsFailed_State
    override protected def _welcomeHidingState          = new NodeInit_WelcomeHiding_GridAdsWait_State
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
  protected trait GridBlockClickStateT extends super.GridBlockClickStateT {
    override def _startFocusOnAdState = new FocStartingForAd
  }
  /** Трейт для поддержки переключения на состояния, исходящие из OnGridStateT  */
  protected trait OnGridStateT extends super.OnGridStateT with GridBlockClickStateT {
    override def _loadModeState = new GridLoadModeState
  }

  /** Превратить search-таб в соответствующее состояние. */
  protected def _searchTab2state(sd1: IStData): FsmState = {
    sd1.search.currTab match {
      case MTabs.Geo      => new OnSearchGeoState
      case MTabs.Tags     => new OnSearchTagsState
    }
  }

  /** Реализация состояния, где карточки уже загружены. */
  class OnPlainGridState extends OnPlainGridStateT with OnGridStateT with _NodeSwitchState {
    override def _nextStateSearchPanelOpened(sd1: MStData) = _searchTab2state(sd1)
    override protected def _navLoadListState = new OnGridNavLoadListState
  }

  class GridLoadModeState extends OnGridLoadingMoreStateT with GridBlockClickStateT {
    override protected def _adsLoadedState = new OnPlainGridState
    override protected def _findAdsFailedState = new OnPlainGridState
  }

  /*--------------------------------------------------------------------------------
   * Состояния нахождения в сетке и на поисковой панели одновременно (grid + search).
   *--------------------------------------------------------------------------------*/
  /** Общий код для реакции на закрытие search-панели. */
  protected[this] trait _SearchClose extends OnSearchStateT {
    override def _nextStateSearchPanelClosed = new OnPlainGridState
  }

  /** Состояние, где и сетка есть, и поисковая панель отрыта на вкладке географии. */
  class OnSearchGeoState extends OnGridSearchGeoStateT with _SearchClose with OnGridStateT {
    override def _tabSwitchedFsmState = new OnSearchTagsState
  }

  /** Состояние, где открыта вкладка хеш-тегов на панели поиска. */
  class OnSearchTagsState extends OnSearchTagsStateT with _SearchClose with OnGridStateT {
    override def _tabSwitchedFsmState = new OnSearchGeoState
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

}
