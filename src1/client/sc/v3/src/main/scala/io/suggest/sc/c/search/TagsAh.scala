package io.suggest.sc.c.search

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MScreen
import io.suggest.grid.GridConst
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{HandleScApiResp, MScRoot}
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionType, MScRespActionTypes}
import io.suggest.sc.styl.ScCss
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import japgolly.univeq._

import scala.util.Success


/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.12.17 15:43
  * Description: Контроллер для тегов. Вынесен из SearchAh, т.к. вся его логика очень изолирована в рамках пары моделей.
  */
class TagsAh[M](
                 api              : IScUniApi,
                 tagsSearchQsRO   : ModelRO[MScQs],
                 screenRO         : ModelRO[MScreen],
                 modelRW          : ModelRW[M, MTagsSearchS]
               )
  extends ActionHandler( modelRW )
  with Log
{

  override def handle: PartialFunction[Any, ActionResult[M]] = {

    // Скроллинг в списке тегов: возможно надо подгрузить ещё тегов.
    case m: TagsScroll =>
      val v0 = value
      // Надо подгружать ещё или нет?
      if (
        !v0.tagsReq.isPending &&
        v0.hasMoreTags && {
          val containerHeight = screenRO.value.height - ScCss.TABS_OFFSET_PX
          val scrollPxToGo = m.scrollHeight - containerHeight - m.scrollTop
          scrollPxToGo < GridConst.LOAD_MORE_SCROLL_DELTA_PX
        }
      ) {
        // Требуется подгрузить ещё тегов.
        val fx = Effect.action {
          DoTagsSearch(clear = false, ignorePending = true)
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
    case m: DoTagsSearch =>
      val v0 = value
      if (m.clear || m.ignorePending || !v0.tagsReq.isPending) {
        // Запустить эффект поиска, выставить запущенный запрос в состояние.
        val req2 = v0.tagsReq.pending()

        val fx = Effect {
          // Подготовить аргументы запроса:
          val args0 = tagsSearchQsRO.value
          val offsetOpt = OptionUtil.maybeOpt(!m.clear) {
            v0.tagsReq
              .toOption
              .map(_.resp.size)
          }
          // TODO Лимит результатов - брать из высоты экрана.
          val limit = 30
          val args2 = args0.withSearch(
            search = args0.search.withLimitOffset(
              limit     = Some(limit),
              offset    = offsetOpt
            )
          )
          // Запустить запрос.
          api
            .pubApi( args2 )
            .transform { tryResp =>
              val action = HandleScApiResp(
                reason        = m,
                tryResp       = tryResp,
                reqTimeStamp  = Some( req2.asInstanceOf[PendingBase].startTime ),
                qs        = args2
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

  }

}


/** Обработчик ответов по тегам. */
class TagsRespHandler
  extends IRespWithActionHandler
{

  private def _withTags(ctx: MRhCtx, tags2: MTagsSearchS): MScRoot = {
    ctx.value0.withIndex(
      ctx.value0.index.withSearch(
        ctx.value0.index.search
          .withTags( tags2 )
      )
    )
  }

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[DoTagsSearch]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.index.search.tags.tagsReq )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot = {
    val t0 = ctx.value0.index.search.tags
    val t2 = t0.withTagsReq(
      t0.tagsReq.fail(ex)
    )
    _withTags(ctx, t2)
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    (raType ==* MScRespActionTypes.SearchNodes) &&
      isMyReqReason(ctx)
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): (MScRoot, Option[Effect]) = {
    val reason = ctx.m.reason.asInstanceOf[DoTagsSearch]
    val t0 = ctx.value0.index.search.tags

    val tagsResp = ra.search.get
    val tagsList2 = if (reason.clear || t0.tagsReq.isEmpty || t0.tagsReq.exists(_.resp.isEmpty)) {
      // Объединять текущий и полученный списки тегов не требуется.
      tagsResp.results
    } else {
      // Требуется склеить все имеющиеся списки тегов
      t0.tagsReq
        .fold(tagsResp.results) { tags0ri =>
          tags0ri.resp ++ tagsResp.results
        }
    }

    val tagsReq2 = t0.tagsReq.ready(
      MSearchRespInfo(
        resp        = tagsList2,
        textQuery = ctx.m.qs.search.textQuery
      )
    )

    val t2 = t0.copy(
      tagsReq     = tagsReq2,
      hasMoreTags = ctx.m.qs.search.limit.get >= tagsResp.results.size,
      selectedId  = OptionUtil.maybeOpt( !reason.clear )( t0.selectedId )
    )

    val v2 = _withTags(ctx, t2)
    (v2, None)
  }

}
