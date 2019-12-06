package io.suggest.sc.c.search

import diode._
import diode.data.{PendingBase, Pot}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MScreenInfo
import io.suggest.grid.GridConst
import io.suggest.maps.c.RcvrMarkersInitAh
import io.suggest.maps.m.{HandleMapReady, InstallRcvrMarkers, RcvrMarkersInit}
import io.suggest.maps.nodes.{MGeoNodesResp, MRcvrsMapUrlArgs}
import io.suggest.maps.u.IAdvRcvrsMapApi
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.grid.GridLoadAds
import io.suggest.sc.m.inx.{MScIndex, MScSideBars, SideBarOpenClose}
import io.suggest.sc.m.{HandleScApiResp, MScRoot, SetErrorState}
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionType, MScRespActionTypes}
import io.suggest.sc.styl.ScCss
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sc.v.search.SearchCss
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.18 10:31
  * Description: Контроллер экшенов гео-таба.
  */
object GeoTabAh {

  /** Макс. кол-во результатов в одном запросе к серверу. */
  private[search] def REQ_LIMIT = 5

  /** Пересборка SearchCSS.
    *
    * @param screenInfo Данные по экрану.
    * @return Инстанс SearchCss.
    */
  def _mkSearchCss(req              : Pot[MSearchRespInfo[MGeoNodesResp]],
                   screenInfo       : MScreenInfo,
                   searchCssOrNull  : SearchCss
                  ): SearchCss = {
    val args2 = MSearchCssProps(
      req        = req,
      screenInfo = screenInfo,
    )
    val isNeedRebuild = searchCssOrNull == null ||
      MSearchCssProps.MSearchCssPropsFastEq.neqv(searchCssOrNull.args, args2)

    if (isNeedRebuild) {
      // Пересобрать CSS.
      SearchCss( args2 )
    } else {
      // Вернуть исходный CSS, т.к. одинаковые args - одинаковые CSS.
      searchCssOrNull
    }
  }


  def scRoot_index_search_geo_LENS = {
    MScRoot.index
      .composeLens( MScIndex.search )
      .composeLens( MScSearch.geo )
  }
  def scRoot_index_search_geo_found_req_LENS = {
    scRoot_index_search_geo_LENS
      .composeLens( MGeoTabS.found )
      .composeLens( MNodesFoundS.req )
  }

}


class GeoTabAh[M](
                   api            : IScUniApi,
                   rcvrsMapApi    : IAdvRcvrsMapApi,
                   screenInfoRO   : ModelRO[MScreenInfo],
                   geoSearchQsRO  : ModelRO[MScQs],
                   rcvrMapArgsRO  : ModelRO[MRcvrsMapUrlArgs],
                   modelRW        : ModelRW[M, MGeoTabS]
                 )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Метод подготовки запуска запроса поиска на сервер за нодами.
    *
    * @param m Инстанс экшена DoNodesSearch.
    * @param v0 Состояние модели.
    * @return ActionResult.
    */
  private def _doNodesSearch(m: DoNodesSearch, v0: MGeoTabS): ActionResult[M] = {
    if (v0.found.req.isPending && !m.ignorePending) {
      // Не логгируем, т.к. это слишком частое явление при запуске системы с открытой панелью поиска.
      //LOG.log( WarnMsgs.REQUEST_STILL_IN_PROGRESS, msg = m )
      noChange

    } else {
      // Надо понять, изменилось ли состояние аргументов поиска?
      // Подготовить аргументы запроса:
      val args0 = geoSearchQsRO.value

      val search2 = {
        val offsetOpt = OptionUtil.maybeOpt(!m.clear) {
          v0.found.req
            .toOption
            .map(_.resp.nodes.length)
        }
        args0.search.withLimitOffset(
          limit     = Some( GeoTabAh.REQ_LIMIT ),
          offset    = offsetOpt
        )
      }

      val args2 = MScQs.search.set(search2)(args0)

      // Запустить поиск, если что-то изменилось.
      if (v0.found.reqSearchArgs contains[MScQs] args2) {
        // Данные для запроса не отличаются от уже запрошенных. Поэтому игнорируем сигнал.
        noChange

      } else {
        // Поисковый запрос действительно нужно организовать.
        val req2 =
          (if (m.clear) Pot.empty else v0.found.req)
          .pending()

        val req2p = req2.asInstanceOf[PendingBase]

        val runReqFx = Effect {
          // Запустить запрос.
          api
            .pubApi( args2 )
            .transform { tryResp =>
              val action = HandleScApiResp(
                reason        = m,
                tryResp       = tryResp,
                reqTimeStamp  = Some( req2p.startTime ),
                qs            = args2,
              )
              Success( action )
            }
        }

        val found2 = v0.found.copy(
          req = req2,
          reqSearchArgs = Some(args2)
        )

        // В зависимости от состояния textQuery, есть два похожих варианта работы: поиск только тегов и поиск всех узлов по имени.
        // Если textQuery.isEmpty, то убедиться, что ресиверы сброшены на исходную
        val rcvrs2 = if (search2.textQuery.isEmpty) {
          // Сбросить rcvrs на закэшированное состояние.
          v0.data.rcvrsCache
        } else {
          v0.mapInit.rcvrs
            .pending( req2p.startTime )
        }
        // Обновить карту ресиверов в состоянии, если инстанс изменился:
        val mapInit2 = if (rcvrs2 !===* v0.mapInit.rcvrs) {
          MMapInitState.rcvrs
            .set( rcvrs2 )(v0.mapInit)
        } else {
          v0.mapInit
        }

        // Обновлённое состояние.
        val v2 = v0.copy(
          found   = found2,
          mapInit = mapInit2,
          css     = GeoTabAh._mkSearchCss(req2, screenInfoRO.value, v0.css)
        )

        updated(v2, runReqFx)
      }
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запустить запрос поиска узлов
    case m: DoNodesSearch =>
      _doNodesSearch(m, value)

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
          val nodeId = mnode.nodeId.get
          // Есть окликнутый узел. Действовать исходя из типа узла.
          if (mnode.ntype ==* MNodeTypes.Tag) {
            // Это тег. Обновить состояние выбранных тегов, искать в плитке.
            val isAlreadySelected = v0.data.selTagIds contains nodeId

            // Снять либо выставить выделение для тега:
            val selTagIds2: Set[String] =
              if (isAlreadySelected) Set.empty // TODO Когда будет поддержка >1 тега, сделать: v0.data.selTagIds - m.nodeId
              else Set(nodeId) // TODO Пока поддерживается только 1 тег макс. Надо в будущем: v0.data.selTagIds + m.nodeId

            val v2 = MGeoTabS.data
              .composeLens( MGeoTabData.selTagIds )
              .set( selTagIds2 )(v0)

            val gridLoadFx = GridLoadAds(clean = true, ignorePending = true).toEffectPure
            val closeSearchFx = SideBarOpenClose(MScSideBars.Search, open = false).toEffectPure

            val fx = gridLoadFx >> closeSearchFx
            updated(v2, fx)

          } else {
            // Это не тег, значит это adn-узел. Надо перейти в выдачу выбранного узла.
            val fx = MapReIndex(mnode.nodeId).toEffectPure
            // Надо сбросить выбранные теги, есть есть.
            if (v0.data.selTagIds.isEmpty) {
              effectOnly(fx)
            } else {
              val v2 = MGeoTabS.data
                .composeLens( MGeoTabData.selTagIds )
                .set( Set.empty )(v0)
              updated(v2, fx)
            }
          }
        }

    case m: NodesScroll =>
      val v0 = value
      // Надо подгружать ещё или нет?
      if (
        !v0.found.req.isPending &&
        v0.found.hasMore && {
          val containerHeight = screenInfoRO.value.screen.wh.height - ScCss.TABS_OFFSET_PX
          val scrollPxToGo = m.scrollHeight - containerHeight - m.scrollTop
          // TODO Нужно выставить правильную delta, т.к. сдвиг плитки сюда никак не относится, и load-more происходит слишком рано.
          scrollPxToGo < GridConst.LOAD_MORE_SCROLL_DELTA_PX
        }
      ) {
        // Подгрузить ещё тегов.
        val action = DoNodesSearch(clear = false, ignorePending = false)
        _doNodesSearch( action, v0 )
      } else {
        noChange
      }

    // Запуск инициализации гео.карты.
    case InitSearchMap =>
      // Сбросить флаг инициализации карты, чтобы гео.карта тоже отрендерилась на экране.
      val v0 = value
      if (!v0.mapInit.ready) {
        val v2 = MGeoTabS.mapInit
          .composeLens( MMapInitState.ready )
          .set( true )(v0)
        updated( v2 )

      } else {
        noChange
      }


    // Перехват инстанса leaflet map и сохранение в состояние.
    case m: HandleMapReady =>
      val v0 = value
      val v2 = MGeoTabS.data
        .composeLens( MGeoTabData.lmap )
        .set( Some(m.map) )(v0)
      updatedSilent( v2 )


    // Сигнал запуска инициализации маркеров с сервера.
    case RcvrMarkersInit =>
      val v0 = value
      if (!v0.data.rcvrsCache.isPending) {
        // Эффект скачивания карты с сервера:
        val fx = RcvrMarkersInitAh.startInitFx( rcvrMapArgsRO(), rcvrsMapApi )
        // silent, т.к. RcvrMarkersR работает с этим Pot как с Option, а больше это никого и не касается.
        val v2 = MGeoTabS.data
          .composeLens( MGeoTabData.rcvrsCache )
          .modify(_.pending())(v0)
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
        data = MGeoTabData.rcvrsCache.set(rcvrsCache2)(v0.data),
        // И сразу залить в основное состояние карты ресиверов, если там нет иных данных.
        mapInit =
          if (v0.mapInit.rcvrs.isEmpty)
            MMapInitState.rcvrs.set( rcvrsCache2 )(v0.mapInit)
          else
            v0.mapInit
      )
      updated( v2 )

  }

}


/** Обработка sc-resp-экшенов. */
class NodesSearchRespHandler( screenInfoRO: ModelRO[MScreenInfo] )
  extends IRespWithActionHandler {

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

    val v2 = GeoTabAh.scRoot_index_search_geo_LENS.modify(
      MGeoTabS.found
        .composeLens( MNodesFoundS.req )
        .set(req2) andThen
      // При ошибках надо обновлять css, иначе ширина может быть неверной.
      MGeoTabS.css.modify { css0 =>
        GeoTabAh._mkSearchCss(
          req             = req2,
          screenInfo      = screenInfoRO.value,
          searchCssOrNull = css0,
        )
      }
    )(ctx.value0)

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
      hasMore = hasMore
      // TODO Обновлять search-args? сервер модифицирует запрос, если запрос пришёл из IndexAh.
    )

    val g2 = g0.copy(
      mapInit = mapInit2,
      found = mnf2,
      css = GeoTabAh._mkSearchCss(
        req             = req2,
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
