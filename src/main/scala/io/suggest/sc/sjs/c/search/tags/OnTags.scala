package io.suggest.sc.sjs.c.search.tags

import io.suggest.sc.sjs.m.mgeo.NewGeoLoc
import io.suggest.sc.sjs.m.msearch.TagRowClick
import io.suggest.sc.sjs.m.mtags.MTagsSd
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

    protected def REQ_TAGS_LIMIT = 20

    override def afterBecome(): Unit = {
      super.afterBecome()

      val sd0 = _stateData
      if ( !sd0.loadingDone && sd0.loadedCount == 0 ) {
        // Запустить поисковый реквест, чтобы заполнить список тегов начальными данными.
        _ftsLetsStartRequest()
      }
    }

    /** Запуск поискового запроса в рамках текущего состояния. */
    protected def _ftsLetsStartRequest(): Unit = {
      val sd0 = _stateData
      for {
        sinput <- SInput.find()
      } {
        val args = MTagSearchArgs(
          faceFts = sinput.getTextOpt,
          limit   = Some( REQ_TAGS_LIMIT ),
          offset  = Some( sd0.loadedCount )
        )
        val fut = MTagsSearch.search(
          route = routes.controllers.Sc.tagsSearch( args.toJson )
        )
        val reqTstamp = _sendFutResBackTimestamped(fut, MTagSearchRespTs)
        _stateData = sd0.copy(
          currReqTs = Some(reqTstamp)
        )
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

      // Изменилось местоположение выдачи.
      case ngl: NewGeoLoc =>


      // TODO Нужна реакция на скроллинг списка тегов вниз: запуск подгрузки ещё тегов.
    }

    def _handleVisibilityChanged(vis: Visible): Unit = {
      if (!vis.isVisible)
        become( _hiddenState )
    }

    /** Состояние пребывания списка тегов в сокрытом от глаз виде. */
    protected def _hiddenState: FsmState


    /** Реакция на получение ответа сервера по вопросу поиска тегов. */
    protected def _handleSearchRespTs(resp: MTagSearchResp, tstamp: Long): Unit = {
      val sd0 = _stateData
      if ( sd0.currReqTs.contains(tstamp) ) {
        // Получен ожидаемый результат поискового запроса тегов.
        for {
          stList <- StList.find()
        } {
          // Очистить список тегов, если там есть какой-то мусор.
          // Такое возможно, если произошла слишком резкая смена состояния, например в _hadleNewScLoc().
          if (sd0.loadedCount == 0 && stList.nonEmpty) {
            stList.clear()
          }

          // Отрендерить полученные теги в список тегов.
          for (render <- resp.render) {
            stList.appendElements(render)
          }

          // Обновить состояние новыми данными.
          _stateData = sd0.copy(
            loadedCount = sd0.loadedCount + resp.foundCount,
            loadingDone = resp.foundCount < REQ_TAGS_LIMIT,
            currReqTs   = None
          )
        }
      } else {
        warn( WarnMsgs.TAG_SEARCH_XHR_TS_DROP + " " + tstamp + " " + sd0.currReqTs )
      }
    }


    /** Внезапная смена локации выдачи во время отображения списка тегов. */
    protected def _handleNewScLoc(): Unit = {
      // Нужно сбросить состояние, запустить запрос тегов к серверу.
      _stateData = MTagsSd()
      _ftsLetsStartRequest()
    }


    /**
      * Реакция на клик по тегу в списке тегов.
      *
      * @param slr Сигнал о клике.
      */
    protected def _tagRowClicked(slr: StListRow): Unit = {
      // TODO Нужно выделить текущий тег в списке, заставить ScFsm перейти в гео-поиск карточек для указанного геотега, скрыть текущую панель.
      error(WarnMsgs.NOT_YET_IMPLEMENTED)
    }

  }

}
