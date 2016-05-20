package io.suggest.sc.sjs.c.scfsm

import io.suggest.sc.sjs.c.scfsm.ust.Url2StateT
import io.suggest.sc.sjs.m.msc.{IScSd, MScSd}
import io.suggest.sc.sjs.m.msearch.MTabs
import io.suggest.sc.sjs.util.logs.ScSjsLogger
import io.suggest.sjs.common.fsm._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: FSM-контроллер для всей выдачи. Собирается из кусков, которые закрывают ту или иную область.
 */
object ScFsm
  extends SjsFsmImpl
  with ScSjsLogger
  with init.Phase
  with node.States
  with grid.Append with grid.Plain with grid.LoadMore
  with OnGridNav
  with search.OnGeo
  with search.tags.Opened
  with foc.Phase
  with Url2StateT
  //with LogBecome
{

  // Инициализируем базовые внутренние переменные.
  override protected var _state: FsmState = new DummyState

  /** Контейнер данных состояния. */
  override protected var _stateData: SD = MScSd()


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
    new NodeInit_Welcome_AdsWait_State
  }


  /*--------------------------------------------------------------------------------
   * Фаза состояний инициализации выдачи конкретного узла (node index).
   *--------------------------------------------------------------------------------*/
  // TODO может перекинуть классы-реализации внутрь node.States + добавить метод для выхода из фазы на OnPlainGrid?

  /** Реализация состояния-получения-обработки индексной страницы. */
  class NodeInit_GetIndex_WaitIndex_State extends NodeInit_GetIndex_WaitIndex_StateT {
    override def _onNodeIndexFailedState  : FsmState      = this
    override def _nodeInitWelcomeState    : FsmState      = new NodeInit_Welcome_AdsWait_State
  }


  /** Инициализация node-выдачи с переходом на панель навигации. */
  class NodeInit_GetIndex_WaitIndex_WithNav_State extends NodeInit_GetIndex_WaitIndex_State {
    override def _nodeInitWelcomeState = new NodeInit_Welcome_AdsWait_WithNav_State
  }
  /** Welcoming node-выдачи с открытой панелью навигации. */
  class NodeInit_Welcome_AdsWait_WithNav_State
    extends NodeInit_Welcome_AdsWait_State
    with OnGridNavLoadListStateT
    with OnGridStateT
  {
    override def _navPanelReadyState            = null
    override def _onHideNavState                = null
    override def _nodeInitDoneState             = new OnGridNavReadyState
  }


  /** Состояние инициализации выдачи узла: приветствие + фоновая подгрузка карточек. */
  class NodeInit_Welcome_AdsWait_State extends NodeInit_Welcome_AdsWait_StateT {
    override def _nodeInitDoneState: FsmState = {
      /*val sd0 = _stateData
      if (sd0.nav.panelOpened) {
        new OnGridNavLoadListState
      } else if (sd0.search.opened) {
        _searchTab2state(sd0)
      } else {*/
        new OnPlainGridState
      //}
    }
  }

  /*--------------------------------------------------------------------------------
   * Фаза состояний работы с сеткой карточек (grid).
   *--------------------------------------------------------------------------------*/
  protected trait GridBlockClickStateT extends super.GridBlockClickStateT with IStartFocusOnAdState

  /** Трейт для поддержки переключения на состояния, исходящие из OnGridStateT  */
  protected trait OnGridStateT extends super.OnGridStateT with GridBlockClickStateT {
    override def _loadMoreState = new GridLoadMoreState
  }

  /** Превратить search-таб в соответствующее состояние. */
  protected def _searchTab2state(sd1: IScSd): FsmState = {
    sd1.search.currTab match {
      case MTabs.Geo      => new OnSearchGeoState
      case MTabs.Tags     => new OnSearchTagsState
    }
  }

  /** Реализация состояния, где карточки уже загружены. */
  class OnPlainGridState extends OnPlainGridStateT with OnGridStateT with _NodeSwitchState {
    override def _nextStateSearchPanelOpened(sd1: MScSd) = _searchTab2state(sd1)
    override protected def _navLoadListState = new OnGridNavLoadListState
  }


  /** Реализация IBackToGridState: выбор состояния для возврата в зависимости от открытости боковых панелей. */
  protected trait IBackToGridState extends super.IBackToGridState {
    override def _backToGridState: FsmState = {
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

  class GridLoadMoreState extends OnGridLoadingMoreStateT with GridBlockClickStateT with IBackToGridState {
    override protected def _adsLoadedState = _backToGridState
    override protected def _findAdsFailedState = _backToGridState
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


}
