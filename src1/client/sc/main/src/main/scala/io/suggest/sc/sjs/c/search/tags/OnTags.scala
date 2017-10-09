package io.suggest.sc.sjs.c.search.tags

import io.suggest.routes.scRoutes
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.m.mgeo.NewGeoLoc
import io.suggest.sc.sjs.m.msearch.TagRowClick
import io.suggest.sc.sjs.m.mtags.{MTagInfo, MTagsSd, TagSelected}
import io.suggest.sc.sjs.vm.search.fts.SInput
import io.suggest.sc.sjs.vm.search.tabs.htag.{StList, StListRow}
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.tags.search._
import io.suggest.sjs.common.fsm.signals.Visible
import io.suggest.routes.JsRoutes_ScControllers._

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
          offset  = Some( sd0.loadedCount ),
          locEnv  = ScFsm.currLocEnv
        )
        val fut = MTagsSearch.search(
          route = scRoutes.controllers.Sc.tagsSearch( MTagSearchArgs.toJson(args) )
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
            LOG.error(ErrorMsgs.TAGS_SEARCH_REQ_FAILED, ex)
        }

      // Клик по тегу в списке тегов.
      case trc: TagRowClick =>
        _handleTagRowClicked(trc)

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
        LOG.warn( WarnMsgs.TAG_SEARCH_XHR_TS_DROP, msg = tstamp + " " + sd0.currReqTs )
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
      * @param trc Сигнал о клике.
      */
    protected def _handleTagRowClicked(trc: TagRowClick): Unit = {

      for (tagNodeId <- trc.row.nodeId) {
        val sd0 = _stateData

        // Узнать id нового текущего тега для обновления состояния и прочего...
        // Попутно выполнить какие-то действия с подстветкой тегов.
        val currTag2: Option[StListRow] = if ( sd0.currTagNodeId.contains(tagNodeId) ) {
          // Клик по уже выбранному тегу. Снять с него выделение, убрать из состояния.
          trc.row.unSelect()
          None

        } else {
          // Клик по тегу в списке, хотя до этого был активен какой-то другой тег.
          // Сначала нужно разВыбрать предыдущий тег:
          for {
            oldTagId  <- sd0.currTagNodeId
            oldTagRow <- StListRow.find(oldTagId)
          } {
            oldTagRow.unSelect()
          }

          // Подсветить свежевыбранный тег
          trc.row.select()

          // Вернуть в состояние новый id тега.
          Some(trc.row)
        }

        // Обновить состояние текущего FSM.
        _stateData = sd0.copy(
          currTagNodeId = currTag2.flatMap(_.nodeId)
        )

        // Уведомить ScFsm о необходимости перенаполнить выдачу карточек.
        ScFsm ! TagSelected(
          info = for (tagRow2 <- currTag2) yield {
            MTagInfo(
              nodeId  = tagNodeId,
              face    = tagRow2.tagFace
            )
          }
        )

      }

    } // _handleTagRowClicked()

  }

}
