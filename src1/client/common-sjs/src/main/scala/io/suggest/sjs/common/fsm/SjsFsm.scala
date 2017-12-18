package io.suggest.sjs.common.fsm

import io.suggest.fsm.{AbstractFsm, AbstractFsmUtil}
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.log.{ILog, Logger}
import io.suggest.sjs.common.model.TimestampedCompanion

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 15:51
 * Description: Трейт для сборки конечных автомата в scala.js.
 */
trait SjsFsm extends AbstractFsm with ILog {

  override type Receive = PartialFunction[Any, Unit]

  override type State_t <: FsmState

  override val LOG: Logger = {
    new Logger( getClass.getSimpleName ) {
      override def fsmState: Option[String] = {
        for (s <- Option(_state)) yield {
          s.name
        }
      }
    }
  }

  /** Минимальный тип сигнала, принимаемый через FSM API. */
  // TODO Нужно реализовать поддержку неточного типа (<: IFsmMsg).
  type FsmMsg_t = IFsmMsg

  override protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive = {
    AbstractFsmUtil.combinePfs(rcvrs)
  }

  /** Текущий обработчик входящих событий. */
  private var _receiver: Receive = _

  /** Выставление указанного ресивера в качестве обработчика событий. */
  override protected def _installReceiver(newReceiver: Receive): Unit = {
    _receiver = newReceiver
  }


  /**
    * Внутренний метод для действий апосля выставления нового состояния.
    * Как минимум, нужно выполнить afterBecome() и выставить ресивер для обработки входящих сигналов.
    */
  override protected def _afterStateChanged(): Unit = {
    try {
      super._afterStateChanged()
    } catch { case ex: Throwable =>
      _state.processFailure(ex)
    }
  }

  /** Добавление слушателя событий отпускания кнопок клавиатуры в состояние. */
  protected trait FsmState extends super.FsmState {

    def receiverPart: Receive

    /** Реакция на проблемы при обработке входящего сообщения. */
    def processEventFailed(e: Any, ex: Throwable): Unit = {
      LOG.error(ErrorMsgs.FSM_MSG_PROCESS_ERROR, ex, e.toString)
    }
    /** Реакция на ошибку в работе состояния. */
    def processFailure(ex: Throwable): Unit = {
      LOG.error(ErrorMsgs.UNEXPECTED_FSM_RUNTIME_ERROR, ex)
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
    // Неожиданные сообщения надо логгировать.
    case other =>
      // Пока только логгируем пришедшее событие. Потом и логгирование надо будет отрубить.
      LOG.log(WarnMsgs.FSM_SIGNAL_UNEXPECTED, msg = other.toString)
  }

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
  final def !!(e: FsmMsg_t): Unit = {
    _sendEventSync(e)
  }

  /** Отправка сообщения с асинхронной отработкой. Управление возвращается назад немедленно. */
  final def !(e: FsmMsg_t): Unit = {
    _sendEvent(e)
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

  /** Реализация синхронной логики "отправки и обработки" сообщений. */
  protected[this] def _sendEventSync(e: Any): Unit = {
    _receiver(e)
  }


  /** Отправка или обработка не удалась. */
  protected[this] def _sendEventFailed(e: Any, ex: Throwable): Unit = {
    LOG.error(ErrorMsgs.SC_FSM_EVENT_FAILED, ex, e.toString)
    _state.processEventFailed(e, ex)
  }


  protected def _retry(afterMs: Long)(f: => State_t): Unit = {
    DomQuick.setTimeout(afterMs) { () =>
      become(f)
    }
  }


  /** Генератор анонимных фунций-коллбеков, использующий wrapper-модель, заданную модель-компаньоном.
    * Завёрнутый результат отправляется в Fsm.
    * @param model Компаньон модели, в которую надо заворачивать исходные данные функции.
    */
  protected def _signalCallbackF[T](model: IFsmMsgCompanion[T]): (T => _) = {
    {arg: T =>
      _sendEventSyncSafe( model(arg) )
    }
  }

  /** Генератор анонимных фунций-коллбеков, пробрасывающих полученные данные как события в ScFsm. */
  protected def _plainCallbackF[T]: (T => _) = {
    {arg: T =>
      _sendEvent(arg)
    }
  }

  /** Подписать фьючерс на отправку результата в ScFsm. */
  protected def _sendFutResBack[T](fut: Future[T])(implicit ec: ExecutionContext): Unit = {
    fut.onComplete { tryRes =>
      val msg = tryRes match {
        case Success(res) => res
        case failure      => failure
      }
      // Вешать асинхронную отправку сюда смысла нет, только паразитные setTimeout() в коде появяться.
      _sendEventSyncSafe(msg)
    }
  }

  /** Подписать фьючерс на отсылку ответа вместе с таймштампом вешанья события.
    * Полезно для определения порядка параллельных одинаковых запросов. */
  protected def _sendFutResBackTimestamped[T](fut: Future[T], model: TimestampedCompanion[T],
                                              timestamp: Long = System.currentTimeMillis()): Long = {
    fut.onComplete { tryRes =>
      val msg = model(tryRes, timestamp)
      _sendEventSyncSafe(msg)
    }
    timestamp
  }

}

/** Реализация [[SjsFsm]] в виде абстрактного класса. В надежде на уменьшение скомпиленного размера.
  * Со scalajs-0.6.x профита это никакого не дало. Может потом будет лучше... */
abstract class SjsFsmImpl extends SjsFsm


/** Добавление поддержки логгирования переключения состояний. */
trait LogBecome extends SjsFsm {

  override protected def become(nextState: State_t): Unit = {
    val oldStateName = _state.name
    super.become(nextState)
    LOG.log(msg = oldStateName + " -> " + _state.name)
  }
}
