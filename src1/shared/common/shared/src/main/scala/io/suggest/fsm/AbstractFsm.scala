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

  /** Тип состояния. Переопределяется для возможности расширения API FsmState
    * в реализации или на пути к ней (в промежуточных трейтах). */
  type State_t <: FsmState
  
  /** Текущее состояние. */
  protected var _state: State_t

  /** Ресивер для всех состояний. */
  protected def allStatesReceiver: Receive

  /**
   * Переключение на новое состояние. Старое состояние будет отброшено.
   *
   * @param nextState Новое состояние.
   *                  null значит, что всё останется на текущем состоянии.
   */
  protected def become(nextState: State_t): Unit = {
    if (nextState != null) {
      _state = nextState
      _afterStateChanged()
    }
  }

  /**
    * Внутренний метод для действий апосля выставления нового состояния.
    * Как минимум, нужно выполнить afterBecome() и выставить ресивер для обработки входящих сигналов.
    */
  protected def _afterStateChanged(): Unit = {
    // Опережающий вызов afterBecome() тут вроде бы нелогичен, но даёт визуальный эффект ускорения работы в scala.js-автоматах.
    _state.afterBecome()
    _installReceiver(_state.receiver)
  }

  /** Выставление указанного ресивера в качестве обработчика событий. */
  protected def _installReceiver(newReceiver: Receive): Unit

  protected def combineReceivers(rcvrs: TraversableOnce[Receive]): Receive

  /** Интерфейс одного состояния. */
  protected trait FsmState {
    def name = getClass.getSimpleName
    def receiverPart: Receive
    /** Сборка общего ресивера из ресивера состояния и allStatesReceiver.
      * Вызывается однократно во время become.
      * Поэтому переопределение через val/lazyval не даст тут никакого прироста. */
    final def receiver: Receive = {
      combineReceivers( Seq(receiverPart, allStatesReceiver) )
    }
    override def toString: String = name

    /** Действия, которые вызываются, когда это состояние выставлено в актор. */
    def afterBecome() {}
  }

  override def toString: String = {
    getClass.getSimpleName + "(" + _state + ")"
  }

}


/** FSM обычно имеет некоторые данные состояния, хранимые в одном immutable-контрейнере.
  * Тут расширение API [[AbstractFsm]] для данной задачи. */
trait StateData extends AbstractFsm {

  /** Тип контейнера stateData. */
  type SD

  /** Геттер и сеттер для stateData. */
  protected var _stateData: SD

  /** become() в выставлением новой stateData перед применением следующего состояния. */
  protected def become(nextState: State_t, sd2: SD): Unit = {
    _stateData = sd2
    become(nextState)
  }
}


object AbstractFsmUtil {

  /** Собрать несколько partial-функций в одну. */
  def combinePfs[A,B](pfs: TraversableOnce[PartialFunction[A, B]]): PartialFunction[A, B] = {
    pfs.reduceLeft(_ orElse _)
  }

}
