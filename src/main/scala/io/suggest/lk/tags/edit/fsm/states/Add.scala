package io.suggest.lk.tags.edit.fsm.states

import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.madd._
import io.suggest.lk.tags.edit.vm.add.{AContainer, ANameInput}
import io.suggest.lk.tags.edit.vm.exist.EContainer
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.msg.ErrorMsgs
import org.scalajs.dom.FormData

import scala.util.Failure

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.09.15 16:16
 * Description: FSM-аддон для сборки состояний процесса добавления тега.
 */
trait Add extends TagsEditFsmStub { fsm =>

  /** Роута для добавления тега. */
  protected def _addTagRoute: Route

  /** Аддон состояния для запуска . */
  trait AddClickedInitT extends FsmState {

    override def afterBecome(): Unit = {
      super.afterBecome()
      // Запустить запрос, если требуется.
      for {
        input <- ANameInput.find()
        econt <- EContainer.find()
      } {
        // Собрать данные для сабмита. Можно собирать также в JSON вместо form/url-encoded.
        val fd = new FormData()

        // Скопировать новое имя тега:
        fd.append(input._underlying.name, input._underlying.value)

        // Залить имена текущих тегов:
        for {
          tagVm <- econt.tagsIterator
          input <- tagVm.input
        } {
          val u = input._underlying
          fd.append(u.name, u.value)
        }

        // Засабмиттить форму через XHR.
        val fut = MTagAdd.add(_addTagRoute, fd)
        _sendFutResBack(fut)
      }
    }

  }


  /** Трейт для сборки состояния, обрабатывающего входящие */
  trait AddRespStateT extends FsmEmptyReceiverState {

    /** Ресивер входящих сигналов. */
    override def receiverPart: Receive = super.receiverPart orElse {
      // Сервер доволен, просит обновить форму
      case ue: IUpdateExisting =>
        _updateExistingTags(ue)

      // Сервер негодует.
      case afe: AddFormError =>
        _handleAddFormError(afe)

      // Очень маловероятные ошибки:
      case ur: UnexpectedResponse =>
        _handleUnexpectedResponse(ur)
      case Failure(ex) =>
        _handleRequestError(ex)
    }

    /** Общий код обновления верстки формы добавляемого имени тега. */
    private def _updateAddCont(ue: IAddFormHtml): Unit = {
      for {
        aCont <- AContainer.find()
        aCont2 <- ue.addFormVm
      } {
        aCont.replaceWith(aCont2)
        aCont2.initLayout(fsm)
        for (input <- aCont2.nameInput) {
          input.focus()
        }
      }
    }

    /** Реакция на запрос обновления текущих тегов. */
    protected def _updateExistingTags(ue: IUpdateExisting): Unit = {
      for (eCont <- EContainer.find()) {
        eCont.setContent( ue.existingHtml )
      }
      _updateAddCont(ue)
      _tagsChanged()
      become(_allDoneState)
    }

    /** Реакция на получение ошибочной формы добавления. */
    protected def _handleAddFormError(afe: AddFormError): Unit = {
      _updateAddCont(afe)
      become(_allDoneState)
    }

    /** Реакция на неожиданный ответ сервера. */
    protected def _handleUnexpectedResponse(ur: UnexpectedResponse): Unit = {
      LOG.error( ErrorMsgs.CANT_ADD_TAG_SERVER_ERROR )
      // TODO Разморозить форму, если замаскирована.
      // TODO Уведомить о какой-то неизвестной проблеме юзера.
      become(_allDoneState)
    }

    /** Реакция на исключение, возникшее при выполнении запроса к серверу. */
    protected def _handleRequestError(ex: Throwable): Unit = {
      LOG.error( ErrorMsgs.CANT_ADD_TAG_SERVER_ERROR, ex )
      // TODO Уведомить юзера о проблеме.
      become(_allDoneState)
    }

    /** Состояние, на которое пора переключаться, когда обработка add-запроса завершена. */
    protected def _allDoneState: FsmState
  }

}
