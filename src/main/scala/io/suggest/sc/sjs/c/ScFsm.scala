package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.scfsm._
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sc.sjs.m.msearch.MTabs
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sc.sjs.vm.grid.GBlock
import io.suggest.sjs.common.util.SjsLogger
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: FSM-контроллер для всей выдачи. Собирается из кусков, которые закрывают ту или иную область.
 */
object ScFsm extends SjsLogger with Init with GetIndex with GridAppend with OnPlainGrid with OnGridSearchGeo
with OnGridSearchHashTags with OnGridNav with foc.StartingForAd {

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

  protected def _retry(afterMs: Long)(f: => FsmState): Unit = {
    dom.window.setTimeout(
      { () => become(f) },
      afterMs
    )
  }

  def firstStart(): Unit = {
    become(new FirstInitState)
  }


  // --------------------------------------------------------------------------------
  // Реализации состояний FSM. Внутреняя логика состояний раскидана по аддонам.
  // --------------------------------------------------------------------------------

  /** Реализация состояния типовой инициализации. */
  class InitState extends InitStateT {
    override def _jsRouterState(jsRouterFut: Future[_]): FsmState = {
      new AwaitJsRouterState(jsRouterFut)
    }
  }
  /** Реализация состояния самой первой инициализации. */
  class FirstInitState extends InitState with FirstInitStateT


  /** Состояние начальной инициализации роутера. */
  class AwaitJsRouterState(
    override val jsRouterFut: Future[_]
  ) extends AwaitJsRouterStateT {

    /** При завершении инициализации js-роутера надо начать инициализацию index'а выдачи. */
    override def finished(): Unit = {
      become( new GetIndexState )
    }

    override def failed(ex: Throwable): Unit = {
      error("JsRouter init failed. Retrying...", ex)
      _retry(250)(new InitState)
    }
  }


  /** Реализация состояния-получения-обработки индексной страницы. */
  class GetIndexState extends GetIndexStateT {

    /** Когда обработка index завершена, надо переключиться на состояние обработки начальной порции карточек. */
    override protected def _onSuccessNextState(findAdsFut: Future[MFindAds], wcHideFut: Future[_], sd1: SD): FsmState = {
      new AppendAdsToGridDuringWelcomeState(findAdsFut, wcHideFut)
    }

    /** Запрос за index'ом не удался. */
    override protected def _onFailure(ex: Throwable): Unit = {
      error("Failed to ask index, retrying", ex)
      _retry(250)(new GetIndexState)
    }
  }


  /** Реализация состояния начальной загрузки карточек в выдачу. */
  class AppendAdsToGridDuringWelcomeState(
    override val findAdsFut: Future[MFindAds],
    override val wcHideFut: Future[_]
  ) extends AppendAdsToGridDuringWelcomeStateT {
    
    override protected def adsLoadedState = new OnPlainGridState

    override protected def _findAdsFailed(ex: Throwable): Unit = ???
  }
  
  
  /** Трейт для поддержки переключения на состояния, исходящие из OnGridStateT  */
  protected trait _FocusOnGridT extends OnGridStateT {
    override protected def _startFocusOnAdState = new FocStartingForAd
  }

  /** Реализация состояния, где карточки уже загружены. */
  class OnPlainGridState extends OnPlainGridStateT with _FocusOnGridT {
    override protected def _nextStateSearchPanelOpened(sd1: MStData): FsmState = {
      sd1.search.currTab match {
        case MTabs.Geo      => new OnGridSearchGeoState
        case MTabs.HashTags => new OnGridSearchHashTagsState
      }
    }

    override protected def _showNavClick(event: Event): Unit = {
      become(new OnGridNavLoadListState)
    }
  }


  // Состояния с search-панелью.
  /** Общий код для реакции на закрытие search-панели. */
  protected[this] trait _SearchClose extends OnGridSearchStateT {
    override protected def _nextStateSearchPanelClosed(sd1: MStData) = new OnPlainGridState
  }

  /** Состояние, где и сетка есть, и поисковая панель отрыта на вкладке географии. */
  class OnGridSearchGeoState extends OnGridSearchGeoStateT with _SearchClose with _FocusOnGridT {
    override protected def _tabSwitchedFsmState(sd2: MStData) = new OnGridSearchHashTagsState
  }

  /** Состояние, где открыта вкладка хеш-тегов на панели поиска. */
  class OnGridSearchHashTagsState extends OnGridSearchHashTagsStateT with _SearchClose with _FocusOnGridT {
    override protected def _tabSwitchedFsmState(sd2: MStData) = new OnGridSearchGeoState
  }


  // Состояния с nav-панелью.
  /** Вспомогательный трейт для сборки  */
  protected trait _OnGridNav extends super._OnGridNav {
    override protected def _onHideNavState(sd1: MStData) = new OnPlainGridState
  }
  /** Состояние отображения панели навигации с текущей подгрузкой списка карточек. */
  class OnGridNavLoadListState extends OnGridNavLoadListStateT with _OnGridNav {
    override protected def _navPanelReadyState = new OnGridNavReadyState
  }
  class OnGridNavReadyState extends OnGridNavReadyStateT with _OnGridNav {
    override protected def _onNodeSwitchState(sd1: MStData) = new GetIndexState
  }


  // Состояния focused-выдачи.
  class FocStartingForAd extends StartingForAdStateT {
  }

}
