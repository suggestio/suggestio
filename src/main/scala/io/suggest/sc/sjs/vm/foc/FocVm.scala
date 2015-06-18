package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.vm.foc.fsm.FocFsm
import io.suggest.sc.sjs.vm.fsm.IFsmMsg
import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: ViewModel для focused-выдачи. Модель проецирует свои вызовы на состояние DOM,
 * а так же является конечным автоматом со своим внутренним состоянием.
 * FSM создает своё состояние при входе в focused-выдачи и разрушает при выходе.
 */
object FocVm extends FocFsm with SjsLogger {

  private class UnfocusedState extends UnfocusedStateT

  /** Контейнер с данными внутреннего FSM-состояния focused-выдачи. */
  override protected var _state: FsmState = new UnfocusedState

  /** Текущий обработчки входящих событий. */
  private var _receiver: Receive = _

  /**
   * Интерфейсная функция для присылания входящих событий в FSM.
   * @param evt Наступившее событие.
   */
  def handleEvent(evt: IFsmMsg): Unit = {
    _receiver(evt)
  }

  /** Выставление указанного ресивера в качестве обработчика событий. */
  override protected def _installReceiver(newReceiver: Receive): Unit = {
    _receiver = newReceiver
  }

  /** Обработчик неожиданных событий. */
  override protected val unexpectedReceiver: Receive = {
    case other =>
      // Пока только логгируем пришедшее событие.
      error("fv wtf: " + other)
  }

  /** Ресивер для всех состояний. */
  override protected def allStatesReceiver: Receive = PartialFunction.empty


  become(new UnfocusedState)

}



