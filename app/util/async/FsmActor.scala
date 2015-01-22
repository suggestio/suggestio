package util.async
import _root_.util.PlayMacroLogsI
import akka.actor.Actor

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 12:22
 * Description: FsmActor -- это трейт для akka.Actor'а, который позволяет быстро описывать акторы конечные автоматы,
 * переключающиеся между акторами-состояниями.
 */

trait FsmActor extends Actor with PlayMacroLogsI {

  /** Текущее состояние. */
  protected var _state: FsmState

  /** Ресивер для всех состояний. */
  def allStatesReceiver: Receive

  /**
   * Переключение на новое состояние. Старое состояние будет отброшено.
   * @param nextState Новое состояние.
   */
  def become(nextState: FsmState): Unit = {
    LOGGER.trace(s"become(): fsm mode switch ${_state} -> $nextState")
    _state.beforeUnbecome()
    _state = nextState
    context.become(_state.receiver, discardOld = true)
    _state.afterBecome()
  }

  /** Ресивер, добавляемый к конец reveive() для всех состояний, чтобы выводить в логи сообщения,
    * которые не были отработаны актором. */
  protected val unexpectedReceiver: Receive = {
    case other =>
      LOGGER.warn(s"${_state.name} Unexpected message dropped [${other.getClass.getName}]:\n  $other")
  }

  /** Интерфейс одного состояния. */
  trait FsmState {
    def name = getClass.getSimpleName
    def receiverPart: Receive
    def superReceiver = allStatesReceiver
    def maybeSuperReceive(msg: Any): Unit = {
      val sr = superReceiver
      if (sr isDefinedAt msg)
        sr(msg)
    }
    def receiver = receiverPart orElse superReceiver orElse unexpectedReceiver
    override def toString: String = name

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    def afterBecome() {}

    /** Действия, которые вызываются, перед тем как это состояние слетает из актора. */
    def beforeUnbecome() {}
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

}


