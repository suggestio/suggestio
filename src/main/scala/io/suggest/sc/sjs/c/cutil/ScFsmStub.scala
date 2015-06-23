package io.suggest.sc.sjs.c.cutil

import io.suggest.fsm.{StateData, AbstractFsm, AbstractFsmUtil}
import io.suggest.sc.sjs.m.mfsm.IFsmMsg
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sjs.common.util.ISjsLogger
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:28
 * Description: Заготовка для сборки FSM-частей подсистем.
 */
trait ScFsmStub extends AbstractFsm with StateData with ISjsLogger {

  override type Receive = PartialFunction[Any, Unit]

  override protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive = {
    AbstractFsmUtil.combinePfs(rcvrs)
  }

  /** Если состояние не требует ресивера, то можно использовать этот трейт. */
  protected trait FsmEmptyReceiverState extends FsmState {
    override def receiverPart: Receive = PartialFunction.empty
  }


  override type SD = MStData
  override protected var _stateData: SD = MStData()

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

}
