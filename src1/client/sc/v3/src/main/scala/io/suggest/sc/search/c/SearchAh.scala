package io.suggest.sc.search.c

import diode._
import diode.data.PendingBase
import io.suggest.common.empty.OptionUtil
import io.suggest.sc.hdr.m.HSearchBtnClick
import io.suggest.sc.search.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick
import japgolly.univeq._
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sc.tags.MScTagsSearchQs
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 15:38
  * Description: Контроллер для общих экшенов поисковой панели.
  */
class SearchAh[M](
                   api            : ISearchApi,
                   searchArgsRO   : ModelRO[MScTagsSearchQs],
                   modelRW        : ModelRW[M, MScSearch]
                 )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по кнопке открытия поисковой панели.
    case HSearchBtnClick =>
      val v2 = value.withIsShown( true )

      if (v2.mapInit.ready) {
        updated( v2 )
      } else {
        val fx = Effect {
          DomQuick
            .timeoutPromiseT(25)(InitSearchMap)
            .fut
        }
        updated( v2, fx )
      }


    // Запуск инициализации гео.карты.
    case InitSearchMap =>
      // Сбросить флаг инициализации карты, чтобы гео.карта тоже отрендерилась на экране.
      val v0 = value

      if (!v0.mapInit.ready) {
        val v2 = v0.withMapInit(
          v0.mapInit
            .withReady(true)
        )
        updated( v2 )

      } else {
        noChange
      }


    // Смена текущего таба на панели поиска.
    case m: SwitchTab =>
      val v0 = value
      if (v0.currTab ==* m.newTab) {
        noChange

      } else {
        // Смена текущей вкладки.
        val v2 = v0.withCurrTab( m.newTab )

        // TODO Если на панель тегов, то надо запустить эффект поиска текущих (начальных) тегов.
        if ((m.newTab ==* MSearchTabs.Tags) && v2.tags.tagsReq.isEmpty) {
          // Переключение на неинициализированную панель тегов: запустить загрузку тегов.
          val fx = Effect.action {
            GetMoreTags(clear = true)
          }
          updated(v2, fx)
        } else {
          // Инициализация тегов не требуется.
          updated(v2)
        }
      }


    // Клик по тегу в списке тегов.
    case m: TagClick =>
      val v0 = value
      val isAlreadySelected = v0.tags.selectedId contains m.nodeId
      // Снять либо выставить выделение для тега.
      val selectedId2 = OptionUtil.maybe( !isAlreadySelected )( m.nodeId )
      val v2 = v0.withTags(
        v0.tags
          .withSelectedId( selectedId2 )
      )
      updated( v2 )


    // Команда к запуску поиска тегов.
    case m: GetMoreTags =>
      val v0 = value
      if (m.clear || !v0.tags.tagsReq.isPending) {
        // Запустить эффект поиска, выставить запущенный запрос в состояние.
        val req2 = v0.tags.tagsReq.pending()

        val fx = Effect {
          // Подготовить аргументы запроса:
          val args0 = searchArgsRO.value
          val offsetOpt = OptionUtil.maybeOpt(!m.clear) {
            v0.tags.tagsReq
              .toOption
              .map(_.size)
          }
          val limit = 30
          val args2 = args0.withLimitOffset( Some(30), offsetOpt = offsetOpt )
          // Запустить запрос.
          api
            .tagsSearch( args2 )
            .transform { tryResp =>
              val action = MoreTagsResp(
                reason    = m,
                timestamp = req2.asInstanceOf[PendingBase].startTime,
                reqLimit  = limit,
                resp      = tryResp
              )
              Success( action )
            }
        }

        val v2 = v0.withTags(
          v0.tags
            .withTagsReq( req2 )
        )
        updated( v2, fx )

      } else {
        // Запрос тегов уже в процессе. Делать ничего не требуется.
        noChange
      }


    // Обработать ответ сервера по вопросу поиска тегов.
    case m: MoreTagsResp =>
      // Сверить timestamp'ы запроса и ответа
      val v0 = value
      val tagsReq0 = v0.tags.tagsReq
      if (tagsReq0 isPendingWithStartTime m.timestamp) {
        // Это ожидаемый запрос. Разбираем результат запроса:
        val tagsReq2 = m.resp.fold(
          tagsReq0.fail,
          {resp =>
            val tagsList2 = if (m.reason.clear || tagsReq0.isEmpty || tagsReq0.exists(_.isEmpty)) {
              // Объединять текущий и полученный списки тегов не требуется.
              resp.tags
            } else {
              // Требуется склеить все имеющиеся списки тегов
              tagsReq0.getOrElse(Nil) ++ resp.tags
            }
            tagsReq0.ready( tagsList2 )
          }
        )
        val v2 = v0.withTags(
          v0.tags.withTagsReq(
            tagsReq2
          )
        )
        updated( v2 )

      } else {
        // Это устаревший ответ, игнорим его:
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }


    // Команда сброса текущего состояния тегов.
    case ResetTags =>
      val v0 = value

      // Требуется ли сразу перезагружать список тегов? Да, если открыта search-панель и вкладка тегов -- текущая.
      val needFxOpt = OptionUtil.maybe( v0.isShown && v0.currTab ==* MSearchTabs.Tags ) {
        Effect.action( GetMoreTags(clear = true) )
      }

      val emptyState = MTagsSearchS.empty
      val v2Opt = OptionUtil.maybe( v0.tags !=* emptyState ) {
        v0.withTags(emptyState)
      }

      this.optionalResult( v2Opt, needFxOpt )

  }

}
