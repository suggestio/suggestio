package io.suggest.sc.sjs.c

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
object ScFsm extends SjsLogger with EarlyFsm with ScIndexFsm with GridFsm {

  // Инициализируем базовые внутренние переменные.
  override protected var _state: FsmState = new DummyState
  override protected var _stateData: SD = MStData()

  /** Текущий обработчик входящих событий. */
  private var _receiver: Receive = _

  /** Выставление указанного ресивера в качестве обработчика событий. */
  override protected def _installReceiver(newReceiver: Receive): Unit = {
    _receiver = newReceiver
  }


  /** Ресивер для всех состояний. */
  override protected val allStatesReceiver: Receive = {
    // TODO Обрабатывать popstate-события (HTML5 History API).
    // TODO Отрабатывать window resize event: пробрасывать в каждое состояние, добавив соотв.API в FsmState.
    case other =>
      // Пока только логгируем пришедшее событие. Потом и логгирование надо будет отрубить.
      log("Drop event: " + other)
  }

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
  protected class AwaitJsRouterState(val jsRouterFut: Future[_]) extends AwaitJsRouterStateT {
    /** При завершении инициализации js-роутера надо начать инициализацию index'а выдачи. */
    override def finished(): Unit = {
      become( new ScIndexState(None) )
    }

    override def failed(ex: Throwable): Unit = {
      error("JsRouter init failed. Retrying...", ex)
      _retry(250)(new InitState)
    }
  }


  /** Реализация состояния-получения-обработки индексной страницы. */
  protected class ScIndexState(val adnIdOpt: Option[String]) extends GetIndexStateT {

    /** Когда обработка index завершена, надо переключиться на следующее состояние. */
    override protected def _onSuccessNextState(findAdsFut: Future[MFindAds], wcHideFut: Future[_], sd1: SD): FsmState = {
      error("TODO _onSuccessFsmState(): adnId=" + adnIdOpt + " " + findAdsFut)
      ???
    }

    /** Запрос за index'ом не удался. */
    override protected def _onFailure(ex: Throwable): Unit = {
      error("Failed to ask index, retrying", ex)
      _retry(250)(new ScIndexState(adnIdOpt))
    }
  }

}
