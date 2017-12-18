package io.suggest.lk.tags.edit.fsm.states

import io.suggest.common.tags.edit.TagsEditConstants.Search.START_SEARCH_TIMER_MS
import io.suggest.lk.tags.edit.fsm.TagsEditFsmStub
import io.suggest.lk.tags.edit.m.signals._
import io.suggest.lk.tags.edit.vm.add.ANameInput
import io.suggest.lk.tags.edit.vm.exist.EDelete
import io.suggest.lk.tags.edit.vm.search.hints.SContainer
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.tags.search.{MTagSearchArgs, MTagSearchRespTs, MTagsSearch}
import org.scalajs.dom.Event

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 13:29
 * Description: Аддон для сборки состяний FSM, связанных с ничего не деланьем.
 */
trait StandBy extends TagsEditFsmStub {

  /** js-роута для поиска тегов. */
  def tagsSearchRoute(args: MTagSearchArgs): Route

  /** Упрощенное состояние ожидания.*/
  protected trait SimpleStandByStateT extends FsmEmptyReceiverState {

    override def receiverPart: Receive = super.receiverPart orElse {

      // Сигнал ввода названия тега с клавиатуры.
      case TagNameTyping(event) =>
        _tagNameTyping(event)
      // Сигнал о том, что юзер выбрал тег в списке тегов-подсказок.
      case tfc: TagFoundClick =>
        _tagFoundClick(tfc)
      // Сигнал для начала поиска тега по имени.
      case StartSearchTimer(ts) =>
        if (_stateData.startSearchTimerTs.contains(ts))
          _onStartTagSearchTimer()
      // Сигнал о завершении поискового запроса к серверу.
      case respTs: MTagSearchRespTs =>
        _handleTagSearchResp(respTs)

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
      // Найти элементы, связанные с удаляемым тегом...
      for {
        edel    <- EDelete.ofEventTarget( event.target )
        etCont  <- edel.tagContainer
      } {
        // Залить текущее значение удаляемого тега в tag name input
        for {
          eti   <- etCont.input
          tname <- eti.value
          tni   <- ANameInput.find()
        } yield {
          tni.value = tname
        }

        // Стереть тег из формы.
        etCont.hideAndRemove()
        _tagsChanged()
      }
    }


    /** Моментальная реакция на ввод текста в поле имени тега. */
    protected def _tagNameTyping(e: Event): Unit = {
      val sd0 = _stateData.maybeClearTimerId()
      _stateData = sd0

      for {
        tnInput <- ANameInput.find()
        tagName <- tnInput.value
        if tagName.trim.length > 0
      } {

        val ts = System.currentTimeMillis()
        val timerId = DomQuick.setTimeout( START_SEARCH_TIMER_MS ) { () =>
          _sendEventSyncSafe(StartSearchTimer(ts))
        }
        _stateData = sd0.copy(
          startSearchTimerId = Some(timerId),
          startSearchTimerTs = Some(ts)
        )
      }
    }


    /** Срабатывания таймаута ожидания ввода тега. */
    protected def _onStartTagSearchTimer(): Unit = {
      for {
        tnInput <- ANameInput.find()
        tagName <- tnInput.value
        if tagName.trim.length > 0
      } {
        // Собираем и запускаем запрос к серверу
        val tagSearchArgs = MTagSearchArgs(
          faceFts = Some(tagName),
          limit   = Some(5)
        )
        val searchFut = MTagsSearch.search(
          route = tagsSearchRoute(tagSearchArgs)
        )
        val searchTstamp = _sendFutResBackTimestamped(searchFut, MTagSearchRespTs)

        _stateData = _stateData.copy(
          startSearchTimerId  = None,
          startSearchTimerTs  = None,
          lastSearchReqTs     = Some(searchTstamp)
        )
      }
    }


    /** Отрабатывание полученного ответа сервера по найденным тегам. */
    protected def _handleTagSearchResp(respTs: MTagSearchRespTs): Unit = {
      val sd0 = _stateData
      if (sd0.lastSearchReqTs.contains( respTs.timestamp )) {

        for {
          cont    <- SContainer.find()
        } {
          respTs.result match {
            case Success(resp) =>
              for {
                render <- resp.render
              } {
                cont.setContent(render)
                cont.show()
              }


            case Failure(ex) =>
              cont.hide()
              LOG.log( WarnMsgs.XHR_RESP_ERROR_STATUS, ex )
          }
        }

        _stateData = sd0.copy(
          lastSearchReqTs = None
        )

      } else {
        LOG.log( WarnMsgs.TAG_SEARCH_XHR_TS_DROP, msg = sd0.lastSearchReqTs + " " + respTs.timestamp)
      }
    }


    /** Реакция на выбор найденного тега в списке. */
    protected def _tagFoundClick(tfc: TagFoundClick): Unit = {
      // Нужно залить содержимое выбранного тега в input и сымитировать на клик по add-кнопке.
      for {
        tface   <- tfc.srow.tagFace
        tnInput <- ANameInput.find()
      } {
        tnInput.value = tface
        _addBtnClicked()
      }
    }

  }

}
