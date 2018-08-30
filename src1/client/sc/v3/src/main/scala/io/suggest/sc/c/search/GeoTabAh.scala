package io.suggest.sc.c.search

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MScreenInfo
import io.suggest.maps.c.RcvrMarkersInitAh
import io.suggest.maps.m.{HandleMapReady, InstallRcvrMarkers, RcvrMarkersInit}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.msg.ErrorMsgs
import io.suggest.routes.IAdvRcvrsMapApi
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.{HandleScApiResp, MScRoot}
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionType, MScRespActionTypes}
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sc.v.search.SearchCss
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.18 10:31
  * Description: Контроллер экшенов гео-таба.
  */
object GeoTabAh {

  /** Пересборка SearchCSS.
    *
    * @param nodesCount Кол-во узлов в списке.
    * @param screenInfo Данные по экрану.
    * @return Инстанс SearchCss.
    */
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
      val req0 = v0.found.req

      if (req0.isPending && !m.ignorePending) {
        // Не логгируем, т.к. это слишком частое явление при запуске системы с открытой панелью поиска.
        //LOG.log( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
        noChange

      } else {
        // Надо понять, изменилось ли состояние аргументов поиска?
        // Подготовить аргументы запроса:
        val args0 = geoSearchQsRO.value

        val search2 = {
          val offsetOpt = OptionUtil.maybeOpt(!m.clear) {
            v0.rcvrsNotCached
              .toOption
              .map(_.resp.nodes.size)
          }
          // TODO Лимит результатов - брать из высоты экрана. Но сервер пока вообще игнорит лимиты для этого поиска...
          val limit = 30
          args0.search.withLimitOffset(
            limit     = Some(limit),
            offset    = offsetOpt
          )
        }

        // Запустить поиск, если что-то изменилось.
        if (v0.found.reqSearchArgs contains search2) {
          // Данные для запроса не отличаются от уже запрошенных. Поэтому игнорируем сигнал.
          noChange

        } else {
          // Поисковый запрос действительно нужно организовать.
          val req2 = req0.pending()
          val req2p = req2.asInstanceOf[PendingBase]

          val runReqFx = Effect {
            val args2 = args0.withSearch(
              search = search2
            )
            // Запустить запрос.
            api
              .pubApi( args2 )
              .transform { tryResp =>
                val action = HandleScApiResp(
                  reason        = m,
                  tryResp       = tryResp,
                  reqTimeStamp  = Some( req2p.startTime ),
                  qs            = args2
                )
                Success( action )
              }
          }

          val found2 = v0.found.withReqWithArgs(
            req = req2,
            reqSearchArgs = Some(search2)
          )

          // В зависимости от состояния textQuery, есть два похожих варианта работы: поиск только тегов и поиск всех узлов по имени.
          // Если textQuery.isEmpty, то убедиться, что ресиверы сброшены на исходную
          val rcvrs2 = if (search2.textQuery.isEmpty) {
            // Сбросить rcvrs на закэшированное состояние.
            // TODO Раньше при сбросе ресайзилась карта, т.к. список слетал полностью. Теперь поиск происходит даже на пустом запросе. Удалить этот коммент?
            //val mapRszFxOpt = for (lmap <- v0.data.lmap) yield
            //  SearchAh.mapResizeFx(lmap)
            v0.data.rcvrsCache
          } else {
            v0.mapInit.rcvrs
              .pending( req2p.startTime )
          }
          // Обновить карту ресиверов в состоянии, если инстанс изменился:
          val mapInit2 = if (rcvrs2 !===* v0.mapInit.rcvrs)
            v0.mapInit.withRcvrs( rcvrs2 )
          else
            v0.mapInit

          // Обновлённое состояние.
          val v2 = v0.copy(
            found   = found2,
            mapInit = mapInit2
          )

          updated(v2, runReqFx)
        }
      }


    // Клик по узлу в списке найденных гео-узлов.
    case m: NodeRowClick =>
      // Найти узел, который был окликнут.
      val v0 = value
      v0.found
        .nodesFoundMap
        .get( m.nodeId )
        .fold {
          // Почему-то не найдено узла, по которому произошёл клик.
          LOG.error( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange
        } { mnode =>
          // Есть окликнутый узел. Действовать исходя из типа узла.
          if (mnode.props.ntype ==* MNodeTypes.Tag) {
            // Это тег. Обновить состояние выбранных тегов, искать в плитке.
            val isAlreadySelected = v0.data.selTagIds contains m.nodeId

            // Снять либо выставить выделение для тега:
            val selTagIds2: Set[String] =
              if (isAlreadySelected) Set.empty // TODO Когда будет поддержка >1 тега, сделать: v0.data.selTagIds - m.nodeId
              else Set(m.nodeId) // TODO Пока поддерживается только 1 тег макс. Надо в будущем: v0.data.selTagIds + m.nodeId

            val v2 = v0.withData(
              v0.data.withSelTagIds( selTagIds2 )
            )
            val fx = Effect.action {
              GridLoadAds(clean = true, ignorePending = true)
            }
            updated(v2, fx)

          } else {
            // Это не тег, значит это adn-узел. Надо перейти в выдачу выбранного узла.
            val fx = Effect.action( MapReIndex(Some(m.nodeId)) )
            // Надо сбросить выбранные теги, есть есть.
            if (v0.data.selTagIds.isEmpty) {
              effectOnly(fx)
            } else {
              val v2 = v0.withData(
                v0.data.withSelTagIds( Set.empty )
              )
              updated(v2, fx)
            }
          }
        }

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
    Some( ctx.value0.index.search.geo.found.req )
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
    raType ==* MScRespActionTypes.SearchNodes
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): (MScRoot, Option[Effect]) = {
    // Накатить результаты в состояние: помимо данных тегов, надо ещё и для карты маркеры подготовить.
    val g0 = ctx.value0.index.search.geo

    val nodesResp = ra.search.get

    // Надо решить, как поступать с исходным списком: конкатенация или перезапись.
    val nodes2 = g0.rcvrsNotCached
      .filter { oldNodes =>
        // Есть какой-то старый ответ, возможно - пустой. Дальше пропускать только НЕ-пустые ответы и если !clear
        val isClear = ctx.m.reason match {
          case dns: DoNodesSearch => dns.clear
          case _ => true
        }
        !isClear && oldNodes.resp.nodes.nonEmpty
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

    // Собрать ресиверы в итог:
    val mapInit2 = textQuery.fold(g0.mapInit) { _ =>
      val msri = MSearchRespInfo(
        textQuery = textQuery,
        resp = MGeoNodesResp(
          nodes = nodes2
        )
      )
      g0.mapInit.withRcvrs(
        g0.mapInit.rcvrs
          .ready( msri )
      )
    }

    // Заполнить/дополнить g0.found найденными элементами.
    val mnf2 = g0.found.copy(
      req = g0.found.req.ready {
        MSearchRespInfo(
          textQuery = textQuery,
          resp      = nodes2
        )
      },
      // TODO Что тут выставлять? Сервер всё пачкой возвращает без учёта limit'а.
      hasMore = false
      // TODO Обновлять search-args? сервер модифицирует запрос, если запрос пришёл из IndexAh.
    )

    val g2 = g0.copy(
      mapInit = mapInit2,
      found = mnf2,
      css = GeoTabAh._mkSearchCss( nodes2.length, getScCssF().args.screenInfo )
    )

    val mapRszFxOpt = for (lmap <- g2.data.lmap) yield
      SearchAh.mapResizeFx(lmap)

    val v2 = _withGeo(ctx, g2)
    (v2, mapRszFxOpt)
  }

}
