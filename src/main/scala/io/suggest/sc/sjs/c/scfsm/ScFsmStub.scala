package io.suggest.sc.sjs.c.scfsm

import io.suggest.fsm.{AbstractFsm, AbstractFsmUtil, StateData}
import io.suggest.sc.sjs.m.mfsm.{KbdKeyUp, IFsmMsg}
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sjs.common.util.ISjsLogger
import org.scalajs.dom.KeyboardEvent

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:28
 * Description: Заготовка для сборки FSM-частей подсистем.
 */
trait ScFsmStub extends AbstractFsm with StateData with ISjsLogger {

  override type Receive = PartialFunction[Any, Unit]

  override type State_t = FsmState

  override protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive = {
    AbstractFsmUtil.combinePfs(rcvrs)
  }



  /** Добавление слушателя событий отпускания кнопок клавиатуры в состояние. */
  protected trait FsmState extends super.FsmState {
    /** Переопределяемый метод для обработки событий клавиатуры.
      * По дефолту -- игнорировать все события клавиатуры. */
    protected def _onKbdKeyUp(event: KeyboardEvent): Unit = {}

    def receiverPart: Receive = {
      case KbdKeyUp(event) =>
        _onKbdKeyUp(event)
    }
  }

  /** Если состояние не требует ресивера, то можно использовать этот трейт. */
  protected trait FsmEmptyReceiverState extends FsmState {
    override def receiverPart: Receive = PartialFunction.empty
  }


  /** Ресивер для всех состояний. */
  override protected def allStatesReceiver: Receive = {
    // Неожиданные сообщения надо логгировать.
    case other =>
      // Пока только логгируем пришедшее событие. Потом и логгирование надо будет отрубить.
      log("[" + _state + "] Dropped event: " + other)
  }

  override type SD = MStData

  /**
   * Статический API-метод для отправки события в FSM:
   *   ScFsm ! event
   * Метод обязан возвращать выполнение вызывающему как можно скорее и без всяких exceptions.
   * @param e Событие.
   */
  final def !(e: IFsmMsg): Unit = {
    _sendEvent(e)
  }

  /** Внутренняя отправка сообщения.
    * Оно может быть любого типа для самоуведомления состояний. */
  protected[this] def _sendEvent(e: Any): Unit = {
    Future {
      _sendEventSync(e)
    } onFailure { case ex =>
      _sendEventFailed(e, ex)
    }
  }

  protected[this] def _sendEventSync(e: Any): Unit


  /** Отправка или обработка не удалась. */
  protected[this] def _sendEventFailed(e: Any, ex: Throwable): Unit = {
    error("!(" + e + ")", ex)
  }


  /** Очень служебное состояние системы, используется когда очень надо. */
  protected[this] class DummyState extends FsmEmptyReceiverState


  /** Интерфейс для метода, дающего состояние переключения на новый узел.
    * Используется для возможности подмешивания реализации в несколько состояний. */
  protected trait INodeSwitchState {
    protected def _onNodeSwitchState: FsmState
  }
}
