package io.suggest.lk.tags.edit.fsm.states

import io.suggest.common.tags.edit.TagsEditConstants
import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.signals._
import io.suggest.lk.tags.edit.vm.add.ANameInput
import io.suggest.lk.tags.edit.vm.exist.EDelete
import org.scalajs.dom
import org.scalajs.dom.{KeyboardEvent, Event}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 13:29
 * Description: Аддон для сборки состяний FSM, связанных с ничего не деланьем.
 */
trait StandBy extends TagsEditFsmStub {

  /** Упрощенное состояние ожидания.*/
  protected trait SimpleStandByStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {
      // Клик по кнопке добавления тега.
      case AddBtnClick(event) =>
        _addBtnClicked()
      // Клик по кнопке удаления тега.
      case DeleteClick(event) =>
        _tagDeleteClick(event)
      // Вызов сабмита списка тегов с клавиатуры (клавиша enter или др.).
      case NameInputSubmit(event) =>
        _addBtnClicked()
    }

    /** Реакция на клик по кнопке добавления тега. */
    protected def _addBtnClicked(): Unit = {
      become(_addBtnClickedState)
    }

    /** Состояние, на которое надо переключаться, после клика по кнопке добавления. */
    protected def _addBtnClickedState: FsmState

    /** Реакция на клик по кнопке удаления тега. */
    protected def _tagDeleteClick(event: Event): Unit = {
      for (edel <- EDelete.maybeApply( event.target )) {
        edel.tagContainer.hideAndRemove()
      }
    }

  }


  /** Трейт состояния, когда ничего не делается. */
  // TODO UNUSED !!! потому что планировалось поиск сделать.
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
      println(event)
      val sd0 = _stateData

      // Отменить старый таймер запуска запроса, если есть.
      for (oldTimerId <- sd0.startSearchTimerId) {
        dom.clearTimeout(oldTimerId)
      }

      val nameInput = ANameInput( event.target.asInstanceOf[ANameInput.Dom_t] )

      // Если текущий текст не пустой, то надо запустить таймер запуска поискового запроса.
      val namePart = nameInput._underlying.value
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
