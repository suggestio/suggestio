package io.suggest.sc.sjs.c.scfsm.search.tags

import io.suggest.sc.sjs.c.scfsm.search.Base
import io.suggest.sc.sjs.m.msearch.{TagRowClick, MTabs}
import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sc.sjs.vm.search.fts.SInput
import io.suggest.sc.sjs.vm.search.tabs.htag.{StListRow, StList}
import io.suggest.sjs.common.msg.{WarnMsgs, ErrorMsgs}
import io.suggest.sjs.common.tags.search.{MTagsSearch, MTagSearchRespTs, MTagSearchResp, MTagSearchArgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 17:19
 * Description: Аддон для сборки состояния нахождения юзера на раскрытой панели поиска со вкладкой тегов.
 */
trait Opened extends Base {

  /** Состояния поиска по хеш-тегам. */
  protected trait OnSearchTagsStateT extends OnSearchStateT {

    override protected def _nowOnTab = MTabs.Tags

    /** Запуск поискового запроса в рамках текущего состояния. */
    override protected def _ftsLetsStartRequest(): Unit = {
      val sd0 = _stateData
      for (sinput <- SInput.find(); ftsState0 <- sd0.search.ftsSearch) {
        val args = MTagSearchArgs(
          faceFts = sinput.getTextOpt,
          limit   = Some(20),
          offset  = Some(ftsState0.offset)
        )
        val fut = MTagsSearch.search(
          route = routes.controllers.MarketShowcase.tagsSearch( args.toJson )
        )
        val lastTstamp = _sendFutResBackTimestamped(fut, MTagSearchRespTs)
        // Видимо timestamp сохранять не надо. В состоянии предусмотрено только сохранение последнего ПОЛУЧЕННОГО timestamp.
      }
    }

    /** Инициализация вкладки. */
    override def afterBecome(): Unit = {
      super.afterBecome()
      // Сразу запустить поисковый запрос.
      _ftsLetsStartRequest()
    }

    override def receiverPart: Receive = super.receiverPart orElse {
      // Сообщение о получение ответа от сервера по поисковому запросу тегов.
      case MTagSearchRespTs(tryResp, tstamp) =>
        tryResp match {
          case Success(resp) =>
            _handleSearchRespTs(resp, tstamp)
          case Failure(ex) =>
            error(ErrorMsgs.TAGS_SEARCH_REQ_FAILED, ex)
        }

      // Клик по тегу в списке тегов.
      case TagRowClick(row) =>
        _tagRowClicked(row)
      // TODO Нужна реакция на скроллинг вниз: подгрузка ещё тегов.
    }

    /** Реакция на получение ответа сервера по вопросу поиска тегов. */
    protected def _handleSearchRespTs(resp: MTagSearchResp, tstamp: Long): Unit = {
      val sd0 = _stateData
      for {
        ftsState0 <- sd0.search.ftsSearch
        if ftsState0.lastRcvdTs.isEmpty || ftsState0.lastRcvdTs.exists(_ < tstamp)
        stList    <- StList.find()
      } {
        // Получен ожидаемый результат поискового запроса тегов.
        stList.clear()
        for (render <- resp.render if resp.foundCount > 0) {
          stList.appendElements(render)
        }
        // Обновить состояние новыми данными. TODO Нужно выставлять no more elements.
        _stateData = sd0.copy(
          search = sd0.search.copy(
            ftsSearch = Some(ftsState0.copy(
              offset      = ftsState0.offset + resp.foundCount,
              lastRcvdTs  = Some(tstamp)
            ))
          )
        )
      }
    }

    /**
     * Реакция на клик по тегу в списке тегов.
      *
      * @param row Ряд тега в списке.
     */
    protected def _tagRowClicked(row: StListRow): Unit = {
      // TODO
      error(WarnMsgs.NOT_YET_IMPLEMENTED)
    }

  }


}
