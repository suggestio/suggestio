package io.suggest.fsm

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.06.15 11:48
 * Description: Абстрактное базовое API для ускорения написания FSM.
 */

trait AbstractFsm {

  /** Тип обработчика приходящих событий. */
  type Receive
  
  /** Текущее состояние. */
  protected var _state: FsmState

  /** Ресивер для всех состояний. */
  protected def allStatesReceiver: Receive

  /**
   * Переключение на новое состояние. Старое состояние будет отброшено.
   * @param nextState Новое состояние.
   */
  protected def become(nextState: FsmState): Unit = {
    _state.beforeUnbecome()
    _state = nextState
    _installReceiver(_state.receiver)
    _state.afterBecome()
  }
  
  /** Выставление указанного ресивера в качестве обработчика событий. */
  protected def _installReceiver(newReceiver: Receive): Unit

  protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive

  /** Интерфейс одного состояния. */
  protected trait FsmState {
    def name = getClass.getSimpleName
    def receiverPart: Receive
    def receiver: Receive = combineReceivers(Seq(receiverPart, allStatesReceiver))
    override def toString: String = name

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    def afterBecome() {}

    /** Действия, которые вызываются, перед тем как это состояние слетает из актора. */
    def beforeUnbecome() {}
  }

}


object AbstractFsmUtil {

  /** Собрать несколько partial-функций в одну. */
  def combinePfs[A,B](pfs: TraversableOnce[PartialFunction[A, B]]): PartialFunction[A, B] = {
    pfs.reduceLeft(_ orElse _)
  }

}
