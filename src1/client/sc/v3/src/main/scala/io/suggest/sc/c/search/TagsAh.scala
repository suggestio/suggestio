package io.suggest.sc.c.search

import diode._
import diode.data.PendingBase
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MScreen
import io.suggest.react.ReactDiodeUtil._
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.search._
import io.suggest.sc.styl.ScCss
import io.suggest.sc.tags.MScTagsSearchQs
import io.suggest.sc.tile.TileConstants
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

import scala.util.Success


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.12.17 15:43
  * Description: Контроллер для тегов. Вынесен из SearchAh, т.к. вся его логика очень изолирована в рамках пары моделей.
  */
class TagsAh[M](
                 api              : ISearchApi,
                 searchArgsRO     : ModelRO[MScTagsSearchQs],
                 screenRO         : ModelRO[MScreen],
                 modelRW          : ModelRW[M, MTagsSearchS]
               )
  extends ActionHandler( modelRW )
  with Log
{

  override val handle: PartialFunction[Any, ActionResult[M]] = {

    // Скроллинг в списке тегов: возможно надо подгрузить ещё тегов.
    case m: TagsScroll =>
      val v0 = value
      // Надо подгружать ещё или нет?
      if (
        !v0.tagsReq.isPending &&
        v0.hasMoreTags && {
          val containerHeight = screenRO.value.height - ScCss.TABS_OFFSET_PX
          val scrollPxToGo = m.scrollHeight - containerHeight - m.scrollTop
          scrollPxToGo < TileConstants.LOAD_MORE_SCROLL_DELTA_PX
        }
      ) {
        // Требуется подгрузить ещё тегов.
        val fx = Effect.action {
          GetMoreTags(clear = false, ignorePending = true)
        }
        val v2 = v0.withTagsReq(
          v0.tagsReq.pending()
        )
        // silent update, чтобы крутилка появилась только после GetMoreTags.
        updatedSilent( v2, fx )

      } else {
        noChange
      }


    // Клик по тегу в списке тегов.
    case m: TagClick =>
      val v0 = value
      val isAlreadySelected = v0.selectedId contains m.nodeId

      // Снять либо выставить выделение для тега.
      val selectedId2 = OptionUtil.maybe( !isAlreadySelected )( m.nodeId )
      val v2 = v0.withSelectedId( selectedId2 )

      // Обновить плитку при выборе тега.
      val fx = Effect.action {
        GridLoadAds(clean = true, ignorePending = true)
      }

      updated( v2, fx )

    // Команда к запуску поиска тегов.
    case m: GetMoreTags =>
      val v0 = value
      if (m.clear || m.ignorePending || !v0.tagsReq.isPending) {
        // Запустить эффект поиска, выставить запущенный запрос в состояние.
        val req2 = v0.tagsReq.pending()

        val fx = Effect {
          // Подготовить аргументы запроса:
          val args0 = searchArgsRO.value
          val offsetOpt = OptionUtil.maybeOpt(!m.clear) {
            v0.tagsReq
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

        val v2 = v0
          .withTagsReq( req2 )

        updated( v2, fx )

      } else {
        // Запрос тегов уже в процессе. Делать ничего не требуется.
        noChange
      }


    // Обработать ответ сервера по вопросу поиска тегов.
    case m: MoreTagsResp =>
      // Сверить timestamp'ы запроса и ответа
      val v0 = value
      val tagsReq0 = v0.tagsReq
      if (tagsReq0 isPendingWithStartTime m.timestamp) {
        // Это ожидаемый запрос. Разбираем результат запроса:
        m.resp.fold(
          {ex =>
            val v2 = v0.withTagsReq(
              tagsReq0.fail(ex)
            )
            updated( v2 )
          },
          {resp =>
            val tagsList2 = if (m.reason.clear || tagsReq0.isEmpty || tagsReq0.exists(_.isEmpty)) {
              // Объединять текущий и полученный списки тегов не требуется.
              resp.tags
            } else {
              // Требуется склеить все имеющиеся списки тегов
              tagsReq0.getOrElse(Nil) ++ resp.tags
            }
            val tagsReq2 = tagsReq0.ready( tagsList2 )
            val v2 = v0.copy(
              tagsReq     = tagsReq2,
              hasMoreTags = m.reqLimit >= resp.tags.size,
              selectedId  = OptionUtil.maybeOpt( !m.reason.clear )( v0.selectedId )
            )
            updated( v2 )
          }
        )


      } else {
        // Это устаревший ответ, игнорим его:
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
