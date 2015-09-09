package io.suggest.lk.tags.edit.fsm.states

import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.madd.{UnexpectedResponse, AddFormError, UpdateExisting, MTagAdd}
import io.suggest.lk.tags.edit.vm.add.{NameInput, Container}
import org.scalajs.dom.FormData

import scala.util.Failure
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 16:16
 * Description: FSM-аддон для сборки состояний процесса добавления тега.
 */
trait Add extends TagsEditFsmStub {

  /** Аддон состояния для запуска . */
  trait AddClickedInitT extends FsmState {

    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить запрос, если требуется.
      for (input <- NameInput.find() if !input.value.isEmpty) {
        val fd = new FormData()
        fd.append(NameInput.DOM_ID, input.value)

        val fut = MTagAdd.add(fd)
        _sendFutResBack(fut)
      }
    }

  }


  /** Трейт для сборки состояния, обрабатывающего входящие */
  trait AddRespStateT extends FsmEmptyReceiverState {

    /** Ресивер входящих сигналов. */
    override def receiverPart: Receive = super.receiverPart orElse {
      case ue: UpdateExisting =>
        _updateExistingTags(ue)
      case afe: AddFormError =>
        _handleAddFormError(afe)
      case ur: UnexpectedResponse =>
        _handleUnexpectedResponse(ur)
      case Failure(ex) =>
        _handleRequestError(ex)
    }

    /** Реакция на запрос обновления текущих тегов. */
    protected def _updateExistingTags(ue: UpdateExisting): Unit = {
      ???
      become(_allDoneState)
    }

    /** Реакция на получение ошибочной формы добавления. */
    protected def _handleAddFormError(afe: AddFormError): Unit = {
      for (cont <- Container.find()) {
        val cont2 = Container( afe.formHtml )
        cont.replaceWith(cont2)
      }
      become(_allDoneState)
    }

    /** Реакция на неожиданный ответ сервера. */
    protected def _handleUnexpectedResponse(ur: UnexpectedResponse): Unit = {
      // TODO Разморозить форму, выдать сообщение о проблеме в логи и уведомить о неизвестной проблеме юзера.
      ???
      become(_allDoneState)
    }

    /** Реакция на исключение, возникшее при выполнении запроса к серверу. */
    protected def _handleRequestError(ex: Throwable): Unit = {
      ???
      become(_allDoneState)
    }

    /** Состояние, на которое пора переключаться, когда обработка add-запроса завершена. */
    protected def _allDoneState: FsmState
  }

}
