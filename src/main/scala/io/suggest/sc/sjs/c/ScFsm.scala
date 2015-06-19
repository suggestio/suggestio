package io.suggest.sc.sjs.c

import io.suggest.sjs.common.util.SjsLogger

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: FSM-контроллер для всей выдачи. Собирается из кусков, которые закрывают ту или иную область.
 */
object ScFsm extends EarlyFsm with FocusedFsm with SjsLogger {

  /** Контейнер с внутренним FSM-состоянием focused-выдачи. */
  override protected var _state: FsmState = new DummyState

  /** Текущий обработчик входящих событий. */
  private var _receiver: Receive = _


  /** Выставление указанного ресивера в качестве обработчика событий. */
  override protected def _installReceiver(newReceiver: Receive): Unit = {
    _receiver = newReceiver
  }


  /** Ресивер для всех состояний. */
  override protected val allStatesReceiver: Receive = {
    // TODO Обрабатывать popstate-события (HTML5 History API).
    // TODO Отрабатывать window resize event: пробрасывать в каждое состояние, добавив соотв.API в FsmState.
    case other =>
      // Пока только логгируем пришедшее событие. Потом и логгирование надо будет отрубить.
      log("Drop event: " + other)
  }

  // Обработать событие синхронно.
  override protected def _sendEventSync(e: Any): Unit = {
    _receiver(e)
  }


  // Из конструктора запускается следующий шаг инициализации. Этот запуск можно делать только один раз.
  firstStart()

}
