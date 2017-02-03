package io.suggest.fsm

import akka.actor.Actor
import io.suggest.util.logs.IMacroLogs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 12:22
 * Description: FsmActor -- это трейт для akka.Actor'а, который позволяет быстро описывать
 * конечные автоматы на базе обычных akka-акторов.
 */

trait FsmActor extends Actor with IMacroLogs with AbstractFsm {

  /**
   * Переключение на новое состояние. Старое состояние будет отброшено.
   * @param nextState Новое состояние.
   */
  override protected def become(nextState: State_t): Unit = {
    LOGGER.trace(s"become(): fsm mode switch ${_state} -> $nextState")
    super.become(nextState)
  }


  /** Выставление указанного ресивера в качестве обработчика событий. */
  override protected def _installReceiver(newReceiver: Receive): Unit = {
    context.become(_state.receiver, discardOld = true)
  }

  /** Ресивер, добавляемый к конец reveive() для всех состояний, чтобы выводить в логи сообщения,
    * которые не были отработаны актором.
    * Любое состояние включает в себя этото ресивер, т.е. минимум один экземпляр всегда есть в памяти.
    * Чтобы было максимум один экземпляр, тут используется val. */
  override protected val allStatesReceiver: Receive = {
    case other =>
      LOGGER.warn(s"${_state.name} Unexpected message dropped [${other.getClass.getName}]:\n  $other")
  }

  /** Состояние-заглушка. Не делает ровным счётом ничего. */
  class DummyState extends FsmState {
    override def receiverPart: Receive = PartialFunction.empty
  }


  /** Команда к самозавершению. */
  protected def harakiri(): Unit = {
    LOGGER.trace("harakiri(). Last state was " + _state.name)
    context stop self
  }

  override protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive = {
    AbstractFsmUtil.combinePfs(rcvrs)
  }

}


