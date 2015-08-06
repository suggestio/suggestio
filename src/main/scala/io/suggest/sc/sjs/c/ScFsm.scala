package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.c.scfsm._
import io.suggest.sc.sjs.m.mgrid.{GridBlockClick, GridScroll}
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sc.sjs.m.msrv.ads.find.MFindAds
import io.suggest.sjs.common.util.SjsLogger
import org.scalajs.dom

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: FSM-контроллер для всей выдачи. Собирается из кусков, которые закрывают ту или иную область.
 */
object ScFsm extends SjsLogger with Init with GetIndex with GridAppend with OnPlainGrid {

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
  protected class InitState extends InitStateT {
    override def _jsRouterState(jsRouterFut: Future[_]): FsmState = {
      new AwaitJsRouterState(jsRouterFut)
    }
  }
  /** Реализация состояния самой первой инициализации. */
  protected class FirstInitState extends InitState with FirstInitStateT


  /** Состояние начальной инициализации роутера. */
  protected class AwaitJsRouterState(
    override val jsRouterFut: Future[_]
  ) extends AwaitJsRouterStateT {

    /** При завершении инициализации js-роутера надо начать инициализацию index'а выдачи. */
    override def finished(): Unit = {
      become( new GetIndexState(None) )
    }

    override def failed(ex: Throwable): Unit = {
      error("JsRouter init failed. Retrying...", ex)
      _retry(250)(new InitState)
    }
  }


  /** Реализация состояния-получения-обработки индексной страницы. */
  protected class GetIndexState(
    override val adnIdOpt: Option[String]
  ) extends GetIndexStateT {

    /** Когда обработка index завершена, надо переключиться на состояние обработки начальной порции карточек. */
    override protected def _onSuccessNextState(findAdsFut: Future[MFindAds], wcHideFut: Future[_], sd1: SD): FsmState = {
      new AppendAdsToGridDuringWelcomeState(findAdsFut, wcHideFut)
    }

    /** Запрос за index'ом не удался. */
    override protected def _onFailure(ex: Throwable): Unit = {
      error("Failed to ask index, retrying", ex)
      _retry(250)(new GetIndexState(adnIdOpt))
    }
  }


  /** Реализация состояния начальной загрузки карточек в выдачу. */
  protected class AppendAdsToGridDuringWelcomeState(
    override val findAdsFut: Future[MFindAds],
    override val wcHideFut: Future[_]
  ) extends AppendAdsToGridDuringWelcomeStateT {
    
    /** Переключение на какое состояние, когда нет больше карточек на сервере? */
    override protected def adsLoadedState: FsmState = {
      new OnPlainGridState
    }

    /** Что делать при ошибке получения карточек. */
    override protected def _findAdsFailed(ex: Throwable): Unit = ???
  }


  /** Реализация состояния, где карточки уже загружены. */
  protected class OnPlainGridState extends OnPlainGridStateT {

    override protected def withAnim = true

    override protected def _nextStateSearchPanelOpened(sd1: MStData): FsmState = {
      ???   // TODO Переключиться на состояние, где карточки и панель поиска открыта.
    }

    /** Обработка кликов по карточкам в сетке. */
    override protected def handleGridBlockClick(gbc: GridBlockClick): Unit = ???

    /** Реакция на вертикальный скроллинг. */
    override protected def handleVScroll(vs: GridScroll): Unit = ???
  }

}
