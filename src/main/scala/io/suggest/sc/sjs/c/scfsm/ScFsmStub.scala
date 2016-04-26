package io.suggest.sc.sjs.c.scfsm

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.m.mfsm.signals.KbdKeyUp
import io.suggest.sc.sjs.m.msc.fsm.MStData
import io.suggest.sjs.common.fsm._
import org.scalajs.dom.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.15 11:28
 * Description: Заготовка для сборки FSM-частей подсистем.
 */
trait ScFsmStub extends SjsFsm with StateData with DirectDomEventHandlerFsm {

  override type State_t = FsmState

  override type SD = MStData

  /** Добавление слушателя событий отпускания кнопок клавиатуры в состояние. */
  protected trait FsmState extends super.FsmState with DirectDomEventHandlerDummy {
    /** Переопределяемый метод для обработки событий клавиатуры.
      * По дефолту -- игнорировать все события клавиатуры. */
    def _onKbdKeyUp(event: KeyboardEvent): Unit = {}
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

  protected def _allStatesReceiver: Receive = {
    // Реакция на события клавиатуры.
    case KbdKeyUp(event) =>
      _state._onKbdKeyUp(event)
  }

  /** Ресивер для всех состояний. */
  override protected def allStatesReceiver: Receive = {
    _allStatesReceiver
      .orElse( super.allStatesReceiver )
  }


  /** Очень служебное состояние системы, используется когда очень надо. */
  protected[this] class DummyState extends FsmEmptyReceiverState


  /** Интерфейс для метода, дающего состояние переключения на новый узел.
    * Используется для возможности подмешивания реализации в несколько состояний. */
  protected trait INodeSwitchState {
    protected def _onNodeSwitchState: FsmState
  }

  // Раскомментить override become() для логгирования переключения состояний:
  /*override protected def become(nextState: FsmState): Unit = {
    log(_state + " -> " + nextState)
    super.become(nextState)
  }*/

}
