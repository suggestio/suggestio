package io.suggest.lk.tags.edit.fsm.states

import io.suggest.common.tags.edit.TagsEditConstants
import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.signals._
import io.suggest.lk.tags.edit.vm.add.NameInput
import org.scalajs.dom
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 13:29
 * Description: Аддон для сборки состяний FSM, связанных с ничего не деланьем.
 */
trait StandBy extends TagsEditFsmStub {

  /** Трейт состояния, когда ничего не делается. */
  protected trait StandByStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Сигнал ввода текста в поле имени.
      case NameInputEvent(event) =>
        _onNameInput(event)

      // Сигнал для начала поиска тега по имени.
      case StartSearchTimer =>
        _onStartSearchTimer()
    }


    /** Реакция на попадание фокуса в инпут ввода имени. */
    protected def _onNameInput(event: Event): Unit = {
      val sd0 = _stateData

      // Отменить старый таймер запуска запроса, если есть.
      for (oldTimerId <- sd0.startSearchTimerId) {
        dom.clearTimeout(oldTimerId)
      }

      val nameInput = NameInput( event.target.asInstanceOf[NameInput.Dom_t] )

      // Если текущий текст не пустой, то надо запустить таймер запуска поискового запроса.
      val namePart = nameInput.value
      if (!namePart.trim.isEmpty) {
        // Перейти на состояние ожидания таймера перед запросом поиска тегов.
        val timerId = dom.setTimeout(
          { () => _sendEventSyncSafe(StartSearchTimer) },
          TagsEditConstants.START_SEARCH_TIMER_MS
        )
        _stateData = sd0.copy(
          startSearchTimerId = Some(timerId)
        )

      } else {
        _stateData = sd0.maybeClearTimerId()
      }
    }

    /** Реакция на срабатывание таймера запуска поиска. */
    protected def _onStartSearchTimer(): Unit = {
      val sd1 = _stateData.maybeClearTimerId()
      become(_startWaitSearchRequestState, sd1)
    }

    /** Состояние запуска и ожидания ответа с поиского запроса. Вместе с ожиданием возможного инпута. */
    protected def _startWaitSearchRequestState: FsmState

  }

}
