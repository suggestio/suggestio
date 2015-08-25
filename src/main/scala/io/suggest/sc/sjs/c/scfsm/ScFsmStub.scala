package io.suggest.sc.sjs.c.scfsm

import io.suggest.fsm.{AbstractFsm, AbstractFsmUtil, StateData}
import io.suggest.sc.sjs.m.mfsm.{IFsmMsgCompanion, IFsmMsg}
import io.suggest.sc.sjs.m.mfsm.signals.KbdKeyUp
import io.suggest.sc.sjs.m.mgeo.{IGeoErrorSignal, IGeoLocSignal}
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sjs.common.controller.fsm.{DirectDomEventHandlerDummy, DirectDomEventHandlerFsm}
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.util.ISjsLogger
import org.scalajs.dom
import org.scalajs.dom.KeyboardEvent

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.Success

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

    /** Реакция на получение данных геолокации. */
    def _geoLocReceived(gs: IGeoLocSignal): Unit = {}
    /** Реакция на получение ошибки получения геолокация. */
    def _geoLocErrorReceived(ge: IGeoErrorSignal): Unit = {
      val e = ge.error
      warn(ErrorMsgs.BSS_GEO_LOC_FAILED + " [" + e.code + "] " + e.message)
    }
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
    // Данные по геолокации от браузера могут приходить когда угодно.
    case gs: IGeoLocSignal =>
      _state._geoLocReceived(gs)
    case ge: IGeoErrorSignal =>
      _state._geoLocErrorReceived(ge)
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

  protected def _retry(afterMs: Long)(f: => FsmState): Unit = {
    dom.window.setTimeout(
      { () => become(f) },
      afterMs
    )
  }


  /** Генератор анонимных фунций-коллбеков, использующий wrapper-модель, заданную модель-компаньоном.
    * Завёрнутый результат отправляется в ScFsm.
    * @param model Компаньон модели, в которую надо заворачивать исходные данные функции.
    */
  protected def _signalCallbackF[T](model: IFsmMsgCompanion[T]): (T => _) = {
    {arg: T =>
      _sendEvent( model(arg) )
    }
  }

  /** Генератор анонимных фунций-коллбеков, пробрасывающих полученные данные как события в ScFsm. */
  protected def _plainCallbackF[T]: (T => _) = {
    {arg: T =>
      _sendEvent(arg)
    }
  }

  /**
   * Переключение на новое состояние. Старое состояние будет отброшено.
   * @param nextState Новое состояние.
   */
  override protected def become(nextState: FsmState): Unit = {
    log(_state.name + " -> " + nextState.name)
    super.become(nextState)
  }

  /** Подписать фьючерс на отправку результата в ScFsm. */
  protected def _sendFutResBack[T](fut: Future[T]): Unit = {
    fut onComplete { case tryRes =>
      val msg = tryRes match {
        case Success(res) => res
        case failure      => failure
      }
      // Вешать асинхронную отправку сюда смысла нет, только паразитные setTimeout() в коде появяться.
      _sendEventSyncSafe(msg)
    }
  }

}
