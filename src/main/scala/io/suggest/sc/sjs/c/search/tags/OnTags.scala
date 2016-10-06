package io.suggest.sc.sjs.c.search.tags

import io.suggest.sc.sjs.m.msearch.TagRowClick
import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sc.sjs.vm.search.fts.SInput
import io.suggest.sc.sjs.vm.search.tabs.htag.{StList, StListRow}
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.tags.search.{MTagSearchArgs, MTagSearchResp, MTagSearchRespTs, MTagsSearch}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.fsm.signals.Visible

import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.16 16:37
  * Description: Код для сборки состояний поддержки тегов.
  */
trait OnTags extends TagsFsmStub {

  /** Поддержка списка тегов, поиска по ним. */
  trait OnTagsStateT extends FsmEmptyReceiverState {

    /** Запуск поискового запроса в рамках текущего состояния. */
    protected def _ftsLetsStartRequest(): Unit = {
      val sd0 = _stateData
      for {
        sinput <- SInput.find()
      } {
        val args = MTagSearchArgs(
          faceFts = sinput.getTextOpt,
          limit   = Some(20),
          offset  = Some(sd0.loadedCount)
        )
        val fut = MTagsSearch.search(
          route = routes.controllers.Sc.tagsSearch( args.toJson )
        )
        val lastTstamp = _sendFutResBackTimestamped(fut, MTagSearchRespTs)
        // Видимо timestamp сохранять не надо. В состоянии предусмотрено только сохранение последнего ПОЛУЧЕННОГО timestamp.
      }
    }


    /** Ресивер состояний. */
    override def receiverPart: Receive = super.receiverPart.orElse {
      case vis: Visible =>
        _handleVisibilityChanged(vis)

      // Сообщение о получение ответа от сервера по поисковому запросу тегов.
      case r: MTagSearchRespTs => //(tryResp, tstamp) =>
        r.result match {
          case Success(resp) =>
            _handleSearchRespTs(resp, r.timestamp)
          case Failure(ex) =>
            error(ErrorMsgs.TAGS_SEARCH_REQ_FAILED, ex)
        }

      // Клик по тегу в списке тегов.
      case TagRowClick(row) =>
        _tagRowClicked(row)

      // TODO Нужна реакция на скроллинг списка тегов вниз: запуск подгрузки ещё тегов.
    }

    def _handleVisibilityChanged(vis: Visible): Unit = {
      val sd0 = _stateData
      if (vis.isVisible && !sd0.loadingDone && sd0.loadedCount == 0) {
        // Запустить поисковый реквест, чтобы заполнить список тегов начальными данными.
        _ftsLetsStartRequest()
      }
    }

    /** Реакция на получение ответа сервера по вопросу поиска тегов. */
    protected def _handleSearchRespTs(resp: MTagSearchResp, tstamp: Long): Unit = {
      val sd0 = _stateData
      if (sd0.lastRcvdTs.isEmpty || sd0.lastRcvdTs.exists(_ < tstamp) ) {
        for {
          stList <- StList.find()
        } {
          // Получен ожидаемый результат поискового запроса тегов.
          //stList.clear()
          for (render <- resp.render) {
            stList.appendElements(render)
          }
          // Обновить состояние новыми данными. TODO Нужно выставлять no more elements.
          _stateData = sd0.copy(
            loadedCount = sd0.loadedCount + resp.foundCount,
            lastRcvdTs = Some(tstamp)
          )
        }
      }
    }


    /**
      * Реакция на клик по тегу в списке тегов.
      *
      * @param row Ряд тега в списке.
      */
    protected def _tagRowClicked(row: StListRow): Unit = {
      // TODO Нужно выделить текущий тег в списке, заставить ScFsm перейти в гео-поиск карточек для указанного геотега, скрыть текущую панель.
      error(WarnMsgs.NOT_YET_IMPLEMENTED)
    }

  }

}
