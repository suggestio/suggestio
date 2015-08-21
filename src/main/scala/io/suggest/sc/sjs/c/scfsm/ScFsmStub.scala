package io.suggest.sc.sjs.c.scfsm

import io.suggest.fsm.{AbstractFsm, AbstractFsmUtil, StateData}
import io.suggest.sc.sjs.m.mfsm.{KbdKeyUp, IFsmMsg}
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sjs.common.controller.fsm.{DirectDomEventHandlerDummy, DirectDomEventHandlerFsm}
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
trait ScFsmStub extends AbstractFsm with StateData with ISjsLogger with DirectDomEventHandlerFsm {

  override type Receive = PartialFunction[Any, Unit]

  override type State_t = FsmState

  override protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive = {
    AbstractFsmUtil.combinePfs(rcvrs)
  }



  /** Добавление слушателя событий отпускания кнопок клавиатуры в состояние. */
  protected trait FsmState extends super.FsmState with DirectDomEventHandlerDummy {
    /** Переопределяемый метод для обработки событий клавиатуры.
      * По дефолту -- игнорировать все события клавиатуры. */
    def _onKbdKeyUp(event: KeyboardEvent): Unit = {}
    def receiverPart: Receive
  }

  /**
   * Если состояние не требует ресивера, то можно использовать этот трейт.
   *
   * Также трейт используется для случаев, когда нужно смешать трейты двух вообще разных состояний,
   * т.е. вместо голого имплемента receiverPart, каждое состояние оверрайдит неабстрактный receiverPart,
   * затем все состояния смешиваются без проблем.
   */
  protected trait FsmEmptyReceiverState extends FsmState {
    override def receiverPart: Receive = PartialFunction.empty
  }


  /** Ресивер для всех состояний. */
  override protected def allStatesReceiver: Receive = {
    // Реакция на события клавиатуры.
    case KbdKeyUp(event) =>
      _state._onKbdKeyUp(event)
    // Неожиданные сообщения надо логгировать.
    case other =>
      // Пока только логгируем пришедшее событие. Потом и логгирование надо будет отрубить.
      log("[" + _state + "] Dropped event: " + other)
  }

  override type SD = MStData

  /**
   * Статический СИНХРОННЫЙ метод API для отправки события в FSM:
   *   ScFsm !! event
   * "!!" вместо "!", т.к. метод блокирующий.
   * Блокирующий, потому что
   * 1. Снижение издержек вызова.
   * 2. Все клиенты этого метода -- по сути короткие асинхронные листенеры. Нет ни одной причины
   * быстро возвращать им поток выполнения и это не потребовалось ни разу.
   * 3. Есть необходимость в поддержании порядка получаемых сообщений совместно с синхронным DirectDomEventHandlerFsm.
   * @param e Событие.
   */
  final def !!(e: IFsmMsg): Unit = {
    _sendEventSync(e)
  }

  /** Внутренняя асинхронная отправка сообщения.
    * Оно может быть любого типа для самоуведомления состояний. */
  protected[this] def _sendEvent(e: Any): Unit = {
    Future {
      _sendEventSyncSafe(e)
    }
  }

  /** Внутренняя синхронная отправка сообщения. */
  protected[this] def _sendEventSyncSafe(e: Any): Unit = {
    try {
      _sendEventSync(e)
    } catch { case ex: Throwable =>
      _sendEventFailed(e, ex)
    }
  }

  /** Реализация синхронной логики отправки сообщения. */
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
