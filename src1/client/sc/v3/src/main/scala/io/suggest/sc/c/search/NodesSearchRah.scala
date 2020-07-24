package io.suggest.sc.c.search

import diode.{ActionResult, Effect, ModelRO}
import diode.data.Pot
import io.suggest.dev.MScreenInfo
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{MScRoot, SetErrorState}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.search.{DoNodesSearch, MGeoTabS, MMapInitState, MNodesFoundS, MSearchRespInfo}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.2020 18:08
  * Description: Обработка sc-resp-экшенов.
  */
class NodesSearchRah(
                      screenInfoRO: ModelRO[MScreenInfo],
                    )
  extends IRespWithActionHandler
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[DoNodesSearch]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( GeoTabAh.scRoot_index_search_geo_found_req_LENS.get(ctx.value0) )
  }

  /** Рендер ошибки идёт прямо в списке узлов. */
  override def handleReqError(ex: Throwable, ctx: MRhCtx): ActionResult[MScRoot] = {
    val req_LENS = GeoTabAh.scRoot_index_search_geo_found_req_LENS

    val errorFx = Effect.action {
      val errDia = MScErrorDia(
        messageCode = ErrorMsgs.XHR_UNEXPECTED_RESP,
        potRO       = Some( ctx.modelRW.zoom( req_LENS.get )),
        retryAction = Some( ctx.m.reason ),
      )
      SetErrorState( errDia )
    }

    val req2 = req_LENS
      .get(ctx.value0)
      .fail(ex)

    val v2 = GeoTabAh.scRoot_index_search_geo_LENS.modify { geo0 =>
      val nodesFound2 = (MNodesFoundS.req set req2)( geo0.found )

      (
        (MGeoTabS.found set nodesFound2 ) andThen
        // При ошибках надо обновлять css, иначе ширина может быть неверной.
        MGeoTabS.css.modify { css0 =>
          GeoTabAh._mkSearchCss(
            nodesFound      = nodesFound2,
            screenInfo      = screenInfoRO.value,
            searchCssOrNull = css0,
          )
        }
      )(geo0)
    }(ctx.value0)

    ActionResult.ModelUpdateEffect(v2, errorFx)
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.SearchNodes
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    // Накатить результаты в состояние: помимо данных тегов, надо ещё и для карты маркеры подготовить.
    val g0 = ctx.value0.index.search.geo

    val nodesResp = ra.search.get

    // Надо решить, как поступать с исходным списком: конкатенация или перезапись.
    val nodes2 = g0
      .rcvrsNotCached
      .filter { oldNodes =>
        // Есть какой-то старый ответ, возможно - пустой. Дальше пропускать только НЕ-пустые ответы и если !clear
        val isClear = ctx.m.reason match {
          case dns: DoNodesSearch => dns.clear
          case _ => true
        }
        !isClear && oldNodes.resp.nodes.nonEmpty
      }
      // Нет старого списка. Просто возвращаем полученные узлы.
      .fold(nodesResp) { oldNodes =>
        // Дописать новые полученные узлы в конец.
        val res0 = oldNodes.resp.nodes
        val n2 = if (nodesResp.nodes.isEmpty) res0
        else res0 ++ nodesResp.nodes
        MGeoNodesResp( n2 )
      }

    val textQuery = ctx.m.qs.search.textQuery

    // Собрать ресиверы в итог:
    val mapInit2 = textQuery.fold(g0.mapInit) { _ =>
      MMapInitState.rcvrs
        .modify(
          _.ready(
            MSearchRespInfo(
              textQuery = textQuery,
              resp      = nodes2
            )
          )
        )(g0.mapInit)
    }

    val req2 = if (nodes2.nodes.isEmpty && textQuery.isEmpty) {
      Pot.empty
    } else {
      g0.found.req.ready {
        MSearchRespInfo(
          textQuery = textQuery,
          resp      = nodes2
        )
      }
    }

    val hasMore = nodesResp.nodes.lengthCompare(GeoTabAh.REQ_LIMIT) >= 0

    // Заполнить/дополнить g0.found найденными элементами.
    val mnf2 = g0.found.copy(
      req     = req2,
      hasMore = hasMore,
      // Чтобы произошло переизмерение высоты в react-measure.
      rHeightPx = g0.found.rHeightPx.pending(),
      visible   = req2.nonEmpty,
      // TODO Обновлять search-args? сервер модифицирует запрос, если запрос пришёл из IndexAh.
    )

    val g2 = g0.copy(
      mapInit = mapInit2,
      found = mnf2,
      css = GeoTabAh._mkSearchCss(
        nodesFound      = mnf2,
        screenInfo      = screenInfoRO.value,
        searchCssOrNull = g0.css
      )
    )

    val mapRszFxOpt = for (lmap <- g2.data.lmap) yield
      SearchAh.mapResizeFx(lmap)

    val v2 = (GeoTabAh.scRoot_index_search_geo_LENS set g2)(ctx.value0)
    ActionResult( Some(v2), mapRszFxOpt )
  }

}
