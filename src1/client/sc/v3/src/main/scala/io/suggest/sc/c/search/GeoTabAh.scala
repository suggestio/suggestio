package io.suggest.sc.c.search

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MScreenInfo
import io.suggest.maps.c.RcvrMarkersInitAh
import io.suggest.maps.m.{HandleMapReady, InstallRcvrMarkers, RcvrMarkersInit}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.msg.ErrorMsgs
import io.suggest.routes.IAdvRcvrsMapApi
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{HandleScApiResp, MScRoot}
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionType, MScRespActionTypes}
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sc.v.search.SearchCss
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import japgolly.univeq._
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.18 10:31
  * Description: Контроллер экшенов гео-таба.
  */
object GeoTabAh {

  def _mkSearchCss(nodesCount: Int, screenInfo: MScreenInfo): SearchCss = {
    SearchCss(
      MSearchCssProps(
        screenInfo = screenInfo,
        nodesFoundShownCount = {
          val l = nodesCount
          OptionUtil.maybe(l > 0)( Math.min(l, 3) )
        }
      )
    )
  }

}


class GeoTabAh[M](
                   api            : IScUniApi,
                   rcvrsMapApi    : IAdvRcvrsMapApi,
                   screenInfoRO   : ModelRO[MScreenInfo],
                   geoSearchQsRO  : ModelRO[MScQs],
                   modelRW        : ModelRW[M, MGeoTabS]
                 )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запустить запрос поиска узлов
    case m: DoNodesSearch =>
      val v0 = value
      val req = v0.mapInit.rcvrs
      if ( !req.isPending || m.clear ) {
        val req2 = req.pending()

        // Подготовить аргументы запроса:
        val args0 = geoSearchQsRO.value

        if (args0.search.textQuery.exists(_.nonEmpty)) {

          // Надо запустить запрос на сервер
          val fx = Effect {
            val offsetOpt = OptionUtil.maybeOpt(!m.clear) {
              v0.rcvrsNotCached
                .toOption
                .map(_.resp.nodes.size)
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
                  qs            = args2
                )
                Success( action )
              }
          }

          // Сохранить в состояние данные по запускаемому запросу:
          val v2 = v0.withMapInit(
            v0.mapInit.withRcvrs( req2 )
          )
          updated( v2, fx )

        } else if (req.nonEmpty || req.isPending || v0.mapInit.rcvrs.nonEmpty) {
          // Пустой запрос для поиска.
          // 1. Карту ресиверов - сбросить ресиверов на общую карту.
          // 2. TODO Надо найти теги, размещённые в текущей точке.
          val v2 = v0.copy(
            mapInit = v0.mapInit.withRcvrs( v0.data.rcvrsCache ),
            found   = MNodesFoundS.empty,
            css     = GeoTabAh._mkSearchCss( 0, screenInfoRO.value )
          )
          val mapRszFxOpt = for (lmap <- v0.data.lmap) yield
            SearchAh.mapResizeFx(lmap)

          ah.updatedMaybeEffect(v2, mapRszFxOpt)

        } else {
          // Ничего сбрасывать и запускать не надо.
          noChange
        }

      } else {
        // Не запускать запрос: запрос уже идёт, видимо.
        noChange
      }

    // Клик по узлу в списке найденных гео-узлов.
    case m: NodeRowClick =>
      val fx = Effect.action( MapReIndex(Some(m.nodeId)) )
      effectOnly(fx)

    case _: NodesScroll =>
      // Пока сервер возвращает все результаты пачкой, без лимитов, реакции никакой не будет.
      noChange

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
      val v2 = v0.withData(
        v0.data
          .withLmap( Some(m.map) )
      )
      updatedSilent( v2 )


    // Сигнал запуска инициализации маркеров с сервера.
    case RcvrMarkersInit =>
      val v0 = value
      if (!v0.data.rcvrsCache.isPending) {
        // Эффект скачивания карты с сервера:
        val fx = RcvrMarkersInitAh.startInitFx(rcvrsMapApi)
        // silent, т.к. RcvrMarkersR работает с этим Pot как с Option, а больше это никого и не касается.
        val v2 = v0.withData(
          v0.data.withRcvrsCache( v0.data.rcvrsCache.pending() )
        )
        updatedSilent( v2, fx )

      } else {
        noChange
      }


    // Результат реквеста карты маркеров пришёл и готов к заливке в карту.
    case m: InstallRcvrMarkers =>
      val v0 = value
      val rcvrsCache0 = v0.data.rcvrsCache
      val rcvrsCache2 = m.tryResp.fold(
        {ex =>
          LOG.error( ErrorMsgs.INIT_RCVRS_MAP_FAIL, msg = m, ex = ex )
          rcvrsCache0.fail(ex)
        },
        {resp =>
          rcvrsCache0.ready(
            MSearchRespInfo(
              textQuery   = None,
              resp        = resp
            )
          )
        }
      )
      val v2 = v0.copy(
        data = v0.data.withRcvrsCache( rcvrsCache2 ),
        // И сразу залить в основное состояние карты ресиверов, если там нет иных данных.
        mapInit = if (v0.mapInit.rcvrs.isEmpty)
          v0.mapInit.withRcvrs( rcvrsCache2 )
        else v0.mapInit
      )
      updated( v2 )

  }

}


/** Обработка sc-resp-экшенов. */
class NodesSearchRespHandler(getScCssF: GetScCssF)
  extends IRespWithActionHandler {

  private def _withGeo(ctx: MRhCtx, geo2: MGeoTabS): MScRoot = {
    ctx.value0.withIndex(
      ctx.value0.index.withSearch(
        ctx.value0.index.search
          .withGeo( geo2 )
      )
    )
  }


  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[DoNodesSearch]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.index.search.geo.mapInit.rcvrs )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot = {
    val t0 = ctx.value0.index.search.geo
    val t2 = t0.withMapInit(
      t0.mapInit.withRcvrs(
        t0.mapInit.rcvrs.fail(ex)
      )
    )
    _withGeo(ctx, t2)
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    (raType ==* MScRespActionTypes.SearchNodes) &&
      isMyReqReason(ctx)
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): (MScRoot, Option[Effect]) = {
    // Накатить результаты в состояние: помимо данных тегов, надо ещё и для карты маркеры подготовить.
    val reason = ctx.m.reason.asInstanceOf[DoNodesSearch]
    val g0 = ctx.value0.index.search.geo

    val nodesResp = ra.search.get

    // Надо решить, как поступать с исходным списком: конкатенация или перезапись.
    val nodes2 = g0.rcvrsNotCached
      .filter { oldNodes =>
        // Есть какой-то старый ответ, возможно - пустой. Дальше пропускать только НЕ-пустые ответы и если !clear
        !reason.clear && oldNodes.resp.nodes.nonEmpty
      }
      .fold {
        // Нет старого списка. Просто возвращаем полученные узлы.
        nodesResp.results
      } { oldNodes =>
        val res0 = oldNodes.resp.nodes
        // Дописать полученные узлы в конец.
        if (nodesResp.results.isEmpty) res0
        else res0 ++ nodesResp.results
      }

    val textQuery = ctx.m.qs.search.textQuery
    val msri = MSearchRespInfo(
      textQuery = textQuery,
      resp = MGeoNodesResp(
        nodes = nodes2
      )
    )

    // Собрать ресиверы в итог:
    val rcvrsPot2 = g0.mapInit.rcvrs.ready( msri )

    // Заполнить/дополнить g0.found найденными элементами.
    val mnf2 = textQuery.fold( MNodesFoundS.empty ) { _ =>
      MNodesFoundS(
        req = for (msri0 <- rcvrsPot2) yield {
          // Надо рассчитать расстояния от юзера до каждой точки.
          /*
          val userLocLatLngOpt = for {
            userLoc <- g0.mapInit.userLoc
            if nodes2.exists(_.shapes.nonEmpty)
          } yield {
            val userLocLl = MapsUtil.geoPoint2LatLng( userLoc.point )
            for (node0 <- nodes2) yield {
              val distancesIter = node0.shapes
                .iterator
                .flatMap( _.centerPoint )
                .map { gp =>
                  val gpLl = MapsUtil.geoPoint2LatLng( gp )
                  gpLl.distanceTo( userLocLl )
                }
              val minDistanceOpt = if (distancesIter.nonEmpty)
                Some( distancesIter.min )
              else
                None
            }
          }*/
          msri0.copy(
            resp = nodes2
          )
        },
        // TODO Что тут выставлять? Сервер всё пачкой возвращает без limit'а.
        hasMore     = false,
        selectedId  = ctx.value0.index.state.currRcvrId
      )
    }

    val g2 = g0.copy(
      mapInit = g0.mapInit
        .withRcvrs( rcvrsPot2 ),
      found = mnf2,
      css = GeoTabAh._mkSearchCss( nodes2.length, getScCssF().args.screenInfo )
    )

    val mapRszFxOpt = for (lmap <- g2.data.lmap) yield
      SearchAh.mapResizeFx(lmap)

    val v2 = _withGeo(ctx, g2)
    (v2, mapRszFxOpt)
  }

}
