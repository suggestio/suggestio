package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.scfsm._
import io.suggest.sc.sjs.c.scfsm.init.Init
import io.suggest.sc.sjs.m.msc.fsm.{IStData, MStData}
import io.suggest.sc.sjs.m.msearch.MTabs
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sjs.common.util.SjsLogger
import org.scalajs.dom.Event

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: FSM-контроллер для всей выдачи. Собирается из кусков, которые закрывают ту или иную область.
 */
object ScFsm extends SjsLogger with Init with GetIndex with GridAppend with OnPlainGrid with OnGridSearchGeo
with OnGridSearchHashTags with OnGridNav with foc.StartingForAd with foc.OnFocus with foc.Closing with foc.SimpleShift
with foc.PreLoading with foc.OnTouch with foc.OnTouchPreload {

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


  // --------------------------------------------------------------------------------
  // Реализации состояний FSM. Внутреняя логика состояний раскидана по аддонам.
  // --------------------------------------------------------------------------------

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
    override protected def _geoFinishedState = new GetIndexState
  }
  /** Маловероятное состояние, когда инициализация геолокации прошла быстрее, чем инициализация jsRouter'а. */
  class Init_JsRouterWaitState extends JsRouterInitReceiveT {
    override def _jsRouterReadyState = new GetIndexState
    override protected def _reInitState = new NormalInitState
  }


  /** Реализация состояния-получения-обработки индексной страницы. */
  class GetIndexState extends GetIndexStateT {

    /** Когда обработка index завершена, надо переключиться на состояние обработки начальной порции карточек. */
    override def _onSuccessNextState(findAdsFut: Future[MFindAds], wcHideFut: Future[_], sd1: SD): FsmState = {
      new AppendAdsToGridDuringWelcomeState(findAdsFut, wcHideFut)
    }

    /** Запрос за index'ом не удался. */
    override def _onFailure(ex: Throwable): Unit = {
      error("Failed to ask index, retrying", ex)
      _retry(250)(new GetIndexState)
    }
  }


  /** Реализация состояния начальной загрузки карточек в выдачу. */
  class AppendAdsToGridDuringWelcomeState(
    override val findAdsFut: Future[MFindAds],
    override val wcHideFut: Future[_]
  ) extends AppendAdsToGridDuringWelcomeStateT {
    
    override def adsLoadedState = new OnPlainGridState

    override def _findAdsFailed(ex: Throwable): Unit = ???
  }
  
  
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

    override def _showNavClick(event: Event): Unit = {
      become(new OnGridNavLoadListState)
    }
  }


  // Состояния с search-панелью.
  /** Общий код для реакции на закрытие search-панели. */
  protected[this] trait _SearchClose extends OnGridSearchStateT {
    override def _nextStateSearchPanelClosed(sd1: MStData) = new OnPlainGridState
  }

  /** Состояние, где и сетка есть, и поисковая панель отрыта на вкладке географии. */
  class OnGridSearchGeoState extends OnGridSearchGeoStateT with _SearchClose with OnGridStateT {
    override def _tabSwitchedFsmState(sd2: MStData) = new OnGridSearchHashTagsState
  }

  /** Состояние, где открыта вкладка хеш-тегов на панели поиска. */
  class OnGridSearchHashTagsState extends OnGridSearchHashTagsStateT with _SearchClose with OnGridStateT {
    override def _tabSwitchedFsmState(sd2: MStData) = new OnGridSearchGeoState
  }


  // Состояния с nav-панелью.
  /** Вспомогательный трейт для сборки  */
  protected trait _OnGridNav extends super._OnGridNav {
    override def _onHideNavState(sd1: MStData) = new OnPlainGridState
  }
  /** Состояние отображения панели навигации с текущей подгрузкой списка карточек. */
  class OnGridNavLoadListState extends OnGridNavLoadListStateT with _OnGridNav with OnGridStateT {
    override def _navPanelReadyState = new OnGridNavReadyState
  }
  protected trait _NodeSwitchState extends INodeSwitchState {
    override def _onNodeSwitchState = new GetIndexState
  }
  class OnGridNavReadyState extends OnGridNavReadyStateT with _OnGridNav with _NodeSwitchState with OnGridStateT


  // Состояния focused-выдачи.
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
