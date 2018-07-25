package io.suggest.sc.c.search

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.common.empty.OptionUtil
import io.suggest.maps.m.HandleMapReady
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{HandleScApiResp, MScRoot}
import io.suggest.sc.m.search.{DoGeoSearch, InitSearchMap, MGeoTabS}
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionType, MScRespActionTypes}
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.18 10:31
  * Description: Контроллер экшенов гео-таба.
  */
class GeoTabAh[M](
                   api            : IScUniApi,
                   geoSearchQsRO  : ModelRO[MScQs],
                   modelRW        : ModelRW[M, MGeoTabS]
                 )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запустить запрос поиска узлов
    case m: DoGeoSearch =>
      val v0 = value
      if ( !v0.nodesSearch.req.isPending || m.clear ) {
        val req2 = v0.nodesSearch.req.pending()

        // Подготовить аргументы запроса:

        val args0 = geoSearchQsRO.value

        if (args0.search.textQuery.exists(_.nonEmpty)) {

          // Надо запустить запрос на сервер
          val fx = Effect {
            val offsetOpt = OptionUtil.maybeOpt(!m.clear) {
              v0.nodesSearch.req
                .toOption
                .map(_.size)
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
                  apiReq        = args2
                )
                Success( action )
              }
          }

          // Сохранить в состояние данные по запускаемому запросу:
          val v2 = v0.withNodesSearch(
            v0.nodesSearch
              .withReq( req2 )
          )
          updated( v2, fx )

        } else if (v0.nodesSearch.req.nonEmpty || v0.nodesSearch.req.isPending || v0.mapInit.rcvrsFound.nonEmpty) {
          // Пустой запрос для поиска. Сбросить состояние поиска.
          val v2 = v0
            .withNodesSearch(
              v0.nodesSearch.withReq( Pot.empty )
            )
            .withMapInit(
              v0.mapInit.withRcvrsFound( Pot.empty )
            )
          updated(v2)
        } else {
          // Ничего сбрасывать и запускать не надо.
          noChange
        }

      } else {
        // Не запускать запрос: запрос уже идёт, видимо.
        noChange
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


    // Перехват инстанса leaflet map и сохранение в состояние.
    case m: HandleMapReady =>
      val v0 = value
      val v2 = v0
        .withLmap( Some(m.map) )
      updatedSilent( v2 )

  }

}


/** Обработка sc-resp-экшенов. */
class GeoSearchRespHandler extends IRespWithActionHandler {

  private def _withGeo(ctx: MRhCtx, geo2: MGeoTabS): MScRoot = {
    ctx.value0.withIndex(
      ctx.value0.index.withSearch(
        ctx.value0.index.search
          .withGeo( geo2 )
      )
    )
  }


  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[DoGeoSearch]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.index.search.geo.nodesSearch.req )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot = {
    val t0 = ctx.value0.index.search.geo
    val t2 = t0.withNodesSearch(
      t0.nodesSearch
        .withReq( t0.nodesSearch.req.fail(ex) )
    )
    _withGeo(ctx, t2)
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    (raType ==* MScRespActionTypes.SearchNodes) &&
      isMyReqReason(ctx)
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): (MScRoot, Option[Effect]) = {
    // Накатить результаты в состояние: помимо данных тегов, надо ещё и для карты маркеры подготовить.
    val reason = ctx.m.reason.asInstanceOf[DoGeoSearch]
    val g0 = ctx.value0.index.search.geo

    val nodesResp = ra.search.get

    // Надо решить, как поступать с исходным списком: конкатенация или перезапись.
    val nodes2 = g0.nodesSearch
      .req
      .filter { oldNodes =>
        // Есть какой-то старый ответ, возможно - пустой.
        // Дальше пропускать только НЕ-пустые ответы и если !clear
        !reason.clear && oldNodes.nonEmpty
      }
      .fold {
        // Нет старого списка. Просто возвращаем полученные узлы.
        nodesResp.results
      } { oldNodes =>
        // Дописать полученные узлы в конец.
        if (nodesResp.results.isEmpty) oldNodes
        else oldNodes ++ nodesResp.results
      }

    val req2 = g0.nodesSearch.req.ready( nodes2 )
    val g2 = g0
      .withNodesSearch(
        g0.nodesSearch.withReq(req2)
      )
      .withMapInit(
        g0.mapInit
          .withRcvrsFound(
            g0.mapInit.rcvrsFound.ready(
              MGeoNodesResp(
                nodes = for (mnode <- nodes2) yield {
                  mnode.propsShapes
                }
              )
            )
          )
      )

    val v2 = _withGeo(ctx, g2)
    (v2, None)
  }

}
