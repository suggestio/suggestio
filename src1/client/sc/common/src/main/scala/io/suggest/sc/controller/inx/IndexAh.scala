package io.suggest.sc.controller.inx

import cordova.plugins.statusbar.CdvStatusBar
import diode._
import diode.data.{Pot, Ready}
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.{MGeoLoc, MGeoLocSources, MGeoPoint, MLocEnv}
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DiodeUtil.Implicits.{ActionHandlerExt, EffectsOps, PotOpsExt}
import io.suggest.sc.ScConstants
import io.suggest.sc.controller.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs, MWelcomeInfo}
import io.suggest.sc.model._
import io.suggest.sc.model.grid.GridLoadAds
import io.suggest.sc.model.inx._
import io.suggest.sc.model.search._
import io.suggest.sc.sc3._
import io.suggest.sc.util.api.IScUniApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.maps.MMapS
import io.suggest.sc.ads.{MAdsSearchReq, MIndexAdOpenQs, MScFocusArgs, MScGridArgs, MScNodesArgs}
import io.suggest.sc.controller.search.SearchAh
import io.suggest.sc.model.dia.err.MScErrorDia
import io.suggest.sc.model.menu.MMenuS
import io.suggest.sc.model.styl.MScCssArgs
import io.suggest.sc.util.{ScGeoUtil, ScQsUtil}
import io.suggest.sc.view.search.SearchCss
import io.suggest.sc.view.styl.ScCss
import io.suggest.spa.DoNothing
import japgolly.univeq._
import scalaz.NonEmptyList

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:39
  * Description: Контроллер index'а выдачи.
  */

object IndexAh {

  /** Значение зума при переходе в узел. */
  final def MAP_ZOOM_ON_NODE = 16

  def _gridReLoadFx(raTypes: Set[MScRespActionType], scSwitchOpt: Option[MScSwitchCtx] = None): Option[Effect] = {
    val afterGridFxOpt = scSwitchOpt.flatMap(_.afterBackGrid)

    if (raTypes contains MScRespActionTypes.AdsTile) {
      afterGridFxOpt

    } else {
      val fx = Effect.action {
        GridLoadAds(
          clean         = true,
          ignorePending = true,
          afterLoadFx   = afterGridFxOpt,
        )
      }
      Some(fx)
    }
  }


  def setMap(center: MGeoPoint, mapRcvrId: Option[String])(v0: MMapS): MMapS = {
    v0.copy(
      centerInit = center,
      centerReal = None,
      // Увеличить зум, чтобы приблизить.
      zoom = mapRcvrId.fold(v0.zoom)(_ => MAP_ZOOM_ON_NODE),
    )
  }


  /** Непосредственное обновление индекса.
    *
    * @param inx Новый индекс.
    * @return ActionResult.
    */
  def indexUpdated(i0: MScIndex, inx: MSc3IndexResp, m: HandleScApiResp, mroot: MScRoot): ActionResult[MScIndex] = {
    // Сайд-эффекты закидываются в этот аккамулятор:
    var fxsAcc = List.empty[Effect]

    // Если в switch оговорён доп.эффект, то запустить эффект.
    for {
      scSwitch      <- m.switchCtxOpt
      afterSwitchFx <- scSwitch.afterIndex
    } {
      fxsAcc ::= afterSwitchFx
    }

    val nextIndexView = MIndexView(
      rcvrId    = inx.nodeId,
      // Если nodeId не задан, то взять гео-точку из qs
      inxGeoPoint = OptionUtil.maybeOpt( inx.nodeId.isEmpty ) {
        // Не используем текущее значение карты, т.к. карта тоже могла измениться:
        m.qs.common.locEnv.geoLoc
          .headOption
          .map(_.point)
      },
      name  = inx.name,
      switchCtxOpt = m.switchCtxOpt,
    )

    val indexViewAction = m.switchCtxOpt.flatMap(_.viewsAction)

    var i1 = i0.copy(
      resp = i0.resp.ready(
        MIndexRespData(
          resp = inx,
          scQs = m.qs,
        )
      ),
      state = i0.state.copy(
        switch = MInxSwitch.empty,
        // Обновить стопку индексов в связи с новым индексом:
        views = if (indexViewAction ==* MScSwitchCtx.ViewsAction.POP) {
          // Переход на шаг назад. Выкинуть верхний view из списка. Предшествующий ему view заменить на собранный выше.
          i0.state
            .views
            .tails
            .tail
            .tailMaybe
            .toOption
            .flatMap(_.headOption)
            .fold( NonEmptyList( nextIndexView ) ) { nel0 =>
              nextIndexView <:: nel0
            }
        } else if ( indexViewAction ==* MScSwitchCtx.ViewsAction.PUSH ) {
          // Погружение в под-индексы... Сохранить старый индекс, чтобы отобразилась кнопка "назад".
          nextIndexView <:: i0.state.views
        } else {
          // index view action == RESET
          NonEmptyList( nextIndexView )
        }
      ),
      search = {
        var s0 = i0.search

        // Выставить полученную с сервера геоточку как текущую.
        // TODO Не сбрасывать точку, если index не изменился.
        s0 = (for {
          mgp <- inx.geoPoint
          if m.switchCtxOpt.fold( MScSwitchCtx.INDEX_MAP_RESET_DFLT )(_.indexMapReset)
          geo_mapInit_state_LENS = MScSearch.geo
            .andThen( MGeoTabS.mapInit )
            .andThen( MMapInitState.state )
          if !(geo_mapInit_state_LENS.get(i0.search).center ~= mgp)
        } yield {
          geo_mapInit_state_LENS
            .modify( IndexAh.setMap(mgp, inx.nodeId) )(s0)
        })
          .getOrElse( s0 )

        // Если возвращена userGeoLoc с сервера, которая запрашивалась и до сих пор она нужна, то её выставить в состояние.
        for {
          _ <- inx.userGeoLoc
          if s0.geo.mapInit.userLoc.isEmpty
        } {
          s0 = MScSearch.geo
            .andThen(MGeoTabS.mapInit)
            .andThen(MMapInitState.userLoc)
            .replace( inx.userGeoLoc )(s0)
        }

        // Если заход в узел с карты, то надо скрыть search-панель.
        if (s0.panel.opened && {
          m.switchCtxOpt.showWelcome &&
          inx.welcome.nonEmpty
        }) {
          s0 = MScSearch.panel
            .andThen( MSearchPanelS.opened )
            .replace( false )(s0)
        }

        // Возможный сброс состояния тегов
        if (s0.text.query.nonEmpty)
          fxsAcc ::= SearchTextChanged("", noWait = true).toEffectPure
        else if (s0.geo.found.nonEmpty) {
          if (s0.panel.opened)
            fxsAcc ::= DoNodesSearch(clear = true, ignorePending = true).toEffectPure
          else
            s0 = MScSearch.geo
              .andThen( MGeoTabS.found )
              .replace( MNodesFoundS.empty )(s0)
        }

        // Сбросить флаг mapInit.loader, если он выставлен.
        if (s0.geo.mapInit.loader.nonEmpty)
          s0 = s0.resetMapLoader

        // Сбросить selTagIds, если изменились.
        val qsTagIds = m.qs.search.tagNodeId
          .map(_.id)
          .toSet

        if (s0.geo.data.selTagIds !=* qsTagIds) {
          s0 = MScSearch.geo
            .andThen( MGeoTabS.data )
            .andThen( MGeoTabData.selTagIds )
            .replace( qsTagIds )(s0)
        }

        s0
      }
    )

    val respActionTypes = m.tryResp.get.respActionTypes
    // Если панель поиск видна, то запустить поиск узлов в фоне.
    if (i1.search.panel.opened && !(respActionTypes contains MScRespActionTypes.SearchNodes))
      fxsAcc ::= SearchAh.reDoSearchFx( ignorePending = false )

    // Нужно огранизовать инициализацию плитки карточек. Для этого нужен эффект:
    for {
      gridReloadFx <- _gridReLoadFx(
        respActionTypes,
        m.switchCtxOpt
          // Клик по блоку с последующим index ad open не должен приводить к эффектам сброса плитки и т.д.
          .filter { _ =>
            !m.switchCtxOpt.exists(_.viewsAction ==* MScSwitchCtx.ViewsAction.PUSH)
          }
      )
    } {
      fxsAcc ::= gridReloadFx
    }

    // Если открыта форма логина, но индекс сообщает, что isLoggedIn, то сразу закрыть форму логина:
    if (i1.isLoggedIn && mroot.dialogs.login.isDiaOpened)
      fxsAcc ::= ScLoginFormShowHide( visible = false ).toEffectPure

    // Инициализация приветствия. Подготовить состояние welcome.
    val mWcSFutOpt = for {
      respData <- i1.resp.toOption
      if {
        // Всякие FailingStale содержат старый ответ и новую ошибку, их надо отсеять:
        !i1.resp.isFailed &&
        // Если index switch ctx не запрещает рендер welcome карточки:
        m.switchCtxOpt.showWelcome
      }

      wcInfo2 <- respData.resp.welcome
      // Не надо отображать текущее приветствие повторно:
      if !(i0.resp.exists(_.resp.welcome contains[MWelcomeInfo] wcInfo2))
    } yield {
      val tstamp = System.currentTimeMillis()

      // Собрать функцию для запуска неотменяемого таймера.
      // Функция возвращает фьючерс, который исполнится через ~секунду.
      val tpFx = Effect {
        WelcomeAh.timeout( ScConstants.Welcome.HIDE_TIMEOUT_MS, tstamp )
      }

      // Собрать начальное состояние приветствия:
      val mWcS = MWelcomeState(
        isHiding    = false,
        timerTstamp = tstamp
      )

      (tpFx, mWcS)
    }

    i1 = MScIndex.welcome
      .replace( mWcSFutOpt.map(_._2) )(i1)

    // Нужно отребилдить ScCss, но только если что-то реально изменилось.
    val scCssArgs2 = MScCssArgs.from( i1.respOpt, mroot.dev.screen.info )
    if (scCssArgs2 !=* i1.scCss.args) {
      // Изменились аргументы. Пора отребилдить ScCss.
      i1 = MScIndex.scCss
        .replace( ScCss( scCssArgs2 ) )(i1)
    }
    // Объединить эффекты плитки и приветствия воедино:
    for (mwc <- mWcSFutOpt)
      fxsAcc ::= mwc._1

    // В зависимости от нового цвета фона, нужно подчинить этому цвету системную статус-панель.
    val plat = mroot.dev.platform
    if (plat.isCordova && plat.isReady)
      fxsAcc ::= osStatusBarColorFx( i1.scCss, usePanelColor = i1.isAnyPanelOpened )

    // Possibly, need to update URL due to changes in index state.
    val afterFx =
      ResetUrlRoute().toEffectPure >>
      // Also, after URL update, need to add newest index into "recents":
      SaveRecentIndex().toEffectPure

    val fxTotal = fxsAcc
      .mergeEffects
      .fold( afterFx )(_ >> afterFx)

    ActionResult( Some(i1), Some(fxTotal) )
  }

  def _inx_state_switch_ask_LENS = MScIndex.state
    .andThen( MScIndexState.switch )
    .andThen( MInxSwitch.ask )

  def _inx_search_panel_opened_LENS = MScIndex.search
    .andThen( MScSearch.panel )
    .andThen( MSearchPanelS.opened )

  def _inx_menu_opened_LENS = MScIndex.menu
    .andThen( MMenuS.opened )


  /** Сборка эффекта окрашивания системной панели статуса. */
  def osStatusBarColorFx(scCss: ScCss, usePanelColor: Boolean): Effect = {
    val bgColorHex =
      if (usePanelColor) scCss.panelBgHex
      else scCss.bgColorCss.value

    Effect.action {
      CdvStatusBar.backgroundColorByHexString( bgColorHex )
      if (scCss.bgIsLight) CdvStatusBar.styleDefault()
      else CdvStatusBar.styleLightContent()
      DoNothing
    }
  }

}


class IndexRah
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[IScIndexRespReason]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.index.resp )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): ActionResult[MScRoot] = {
    val eMsg = ErrorMsgs.GET_NODE_INDEX_FAILED
    logger.error( eMsg, ex, msg = ctx.m )

    val inx_search_geo_LENS = MScIndex.search
      .andThen( MScSearch.geo )
    val ctx_v0_index_LENS = MRhCtx.value0
      .andThen( MScRoot.index )

    var actionsAccF = MScIndex.resp.modify( _.fail(ex) )

    // Если закешированные scQs содержат что-либо, то надо их обнулить. Иначе повторная ошибка будет прожёвана как уже пройденный успех.
    val inx_search_geo_found_LENS = inx_search_geo_LENS
      .andThen( MGeoTabS.found )
    val nodesFound0 = ctx_v0_index_LENS
      .andThen( inx_search_geo_found_LENS )
      .get( ctx )
    var nodesFoundMods = List.empty[MNodesFoundS => MNodesFoundS]

    // Если на панели поиска pending, то его тоже сбросить. Такое бывает, когда происходило перемещение карты.
    if (nodesFound0.req.isPending)
      nodesFoundMods ::= MNodesFoundS.req.modify( _.fail(ex) )

    if (nodesFoundMods.nonEmpty)
      actionsAccF = actionsAccF andThen inx_search_geo_found_LENS.modify( nodesFoundMods.reduce(_ andThen _) )


    // Надо перевести карту в текущее состояние, которое было до ошибки.
    val searchGeoMapInit_LENS = inx_search_geo_LENS
      .andThen( MGeoTabS.mapInit )
    val mapInit0 = ctx_v0_index_LENS
      .andThen( searchGeoMapInit_LENS )
      .get(ctx)

    var mapInitModsF = List.empty[MMapInitState => MMapInitState]

    // Сбросить mapLoader на исходную
    if (mapInit0.loader.nonEmpty)
      mapInitModsF ::= (MMapInitState.loader replace None)

    // Если загрузка нового индекса связана с перемещением карты, то надо отскочить карту назад в текущую позицию.
    // MapReIndex включает в себя OpenMapRcvr из-за особенностей реализации.
    val reason = ctx.m.reason
    if ( reason.isInstanceOf[MapReIndex] ) {
      // найти предыдущую гео-точку в состоянии системы. Она может быть в текущем indexResp или в последнем index state view.
      val i0 = ctx.value0.index

      for {
        prevGeoPoint <- i0
          .respOpt
          .flatMap(_.geoPoint)
          .orElse {
            i0.state.inxGeoPoint
          }
        // Точка действительно изменялась?
        if prevGeoPoint !=* mapInit0.state.center
      } {
        mapInitModsF ::= MMapInitState.state.modify(_.withCenterInitReal(prevGeoPoint))
      }
    }

    // Раскрыть аккамулятор mapInit-изменений
    if (mapInitModsF.nonEmpty)
      actionsAccF = actionsAccF andThen searchGeoMapInit_LENS.modify(mapInitModsF.reduce(_ andThen _))

    val v2 = (MScRoot.index modify actionsAccF)(ctx.value0)
    val errFx = Effect.action {
      val m = MScErrorDia(
        messageCode = eMsg,
        potRO       = Some( ctx.modelRW.zoom(_.index.resp) ),
        retryAction = Some( reason ),
      )
      SetErrorState(m)
    }

    ActionResult.ModelUpdateEffect( v2, errFx )
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean =
    raType ==* MScRespActionTypes.Index


  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val v0 = ctx.value0
    val i0 = v0.index

    // Если нод много, то надо плашку рисовать на экране.
    val resp = ra.search.get
    val isMultiNodeResp = resp.hasManyNodes

    // Сравнивать полученный index с текущим состоянием. Может быть ничего сохранять не надо?
    if (
      !isMultiNodeResp &&
      // Явную принудительную перезагрузку текущего индекса не перемалываем в same index.
      !ctx.m.reason.isInstanceOf[ReGetIndex] &&
      resp.nodes.exists { node =>
        i0.resp.exists { rn =>
          (rn.resp isSamePlace node.props) &&
          (rn.resp isLooksFullySame node.props)
        }
      }
    ) {
      // Индекс, видимо, не изменился - не перезагружать вид:
      var i1 = i0

      // Убрать возможный pending из resp:
      if (i1.resp.isPending) {
        i1 = MScIndex.resp
          .modify(_.ready(i1.resp.get))(i1)
      }

      // Убрать крутилку-preloader с гео-карты:
      if (i1.search.geo.mapInit.loader.nonEmpty)
        i1 = MScIndex.search
          .modify(_.resetMapLoader)(i1)

      val inx_state_switch_ask_LENS = IndexAh._inx_state_switch_ask_LENS
      if ( inx_state_switch_ask_LENS.get(i1).nonEmpty )
        i1 = (inx_state_switch_ask_LENS replace None)( i1 )

      val v2 = (MScRoot.index replace i1)(v0)

      var fxAcc = List.empty[Effect]

      // Запустить эффект обновления плитки, если плитка не пришла автоматом.
      for (fx <- IndexAh._gridReLoadFx(
        ctx.m.tryResp
          .toOption
          .fold( Set.empty[MScRespActionType] )(_.respActionTypes),
        ctx.m.switchCtxOpt,
      ))
        fxAcc ::= fx

      ActionResult( Some(v2), fxAcc.mergeEffects )

    } else if (isMultiNodeResp || ctx.m.switchCtxOpt.exists(_.demandLocTest)) {
      // Это тест переключения выдачи в новое местоположение, и узел явно изменился.
      // Вместо переключения узла, надо спросить юзера, можно ли переходить полученный узел:
      val switchAskState = MInxSwitchAskS(
        okAction  = ctx.m,
        nodesResp = resp,
        searchCss = {
          val nodesFound2 = MNodesFoundS(
            req = Ready( MSearchRespInfo(resp = MGeoNodesResp(resp.nodes)) ),
            hasMore = false,
            visible = true,
          )
          val cssArgs = MSearchCssProps(
            nodesFound  = nodesFound2,
            screenInfo  = ctx.value0.dev.screen.info,
            searchBar   = false,
          )
          SearchCss( cssArgs )
        },
      )

      val v2 = MScRoot.index
        .modify(
          IndexAh._inx_state_switch_ask_LENS replace Some(switchAskState) andThen
          MScIndex.resp.modify( _.unPending )
        )(v0)

      ActionResult.ModelUpdate(v2)

    } else {
      // Индекс изменился, значит заливаем новый индекс в состояние:
      val actRes1 = IndexAh.indexUpdated(
        i0        = v0.index,
        inx       = resp.nodes.head.props,
        m         = ctx.m,
        mroot     = v0,
      )
      ActionResult(
        for (inx2 <- actRes1.newModelOpt) yield {
          (MScRoot.index replace inx2)(v0)
        },
        actRes1.effectOpt
      )
    }
  }

}


/** diode-контроллер. */
class IndexAh[M](
                  api           : IScUniApi,
                  modelRW       : ModelRW[M, MScIndex],
                  rootRO        : ModelRO[MScRoot],
                  scGeoUtil     : => ScGeoUtil,
                )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Непосредственный запуск получения индекса с сервера.
    *
    * @param silentUpdate Не рендерить на экране изменений?
    * @param v0 Исходное значение MScIndex.
    * @param reason Экшен-причина, приведший к запуску запроса.
    * @param switchCtx Данные контекст обновления всей выдачи.
    * @return ActionResult.
    */
  private def _getIndex(silentUpdate: Boolean, v0: MScIndex,
                        reason: IScIndexRespReason, switchCtx: MScSwitchCtx,
                        onChangeFxs: List[Effect] = Nil): ActionResult[M] = {
    val ts = System.currentTimeMillis()
    val root = rootRO.value

    val isSearchNodes = root.index.search.panel.opened

    // Допускать доп.барахло в ответе (foc-карточек, список нод, список плитки и т.д.)? Да, кроме фоновой проверки геолокации.
    val withStuff = !switchCtx.demandLocTest

    val args = MScQs(
      common = MScCommonQs(
        apiVsn = root.internals.conf.apiVsn,
        // Когда уже задан id-ресивера, не надо слать на сервер маячки и географию.
        locEnv = {
          // Слать на сервер координаты с карты или с gps, если идёт определение местоположения.
          if (switchCtx.forceGeoLoc.nonEmpty)
            MLocEnv(switchCtx.forceGeoLoc, root.locEnvRadioBeacons)
          else if (switchCtx.demandLocTest)
            root.locEnvUser
          else if (switchCtx.indexQsArgs.nodeId.isEmpty)
            root.locEnvMap
          else
            MLocEnv.empty
        },
        screen = Some( root.dev.screen.info.screen ),
      ),
      index = Some( switchCtx.indexQsArgs ),
      // Фокусироваться надо при запуске. Для этого следует получать всё из reason, а не из состояния.
      foc = for {
        focAdId <- switchCtx.focusedAdId
        if withStuff
      } yield {
        MScFocusArgs(
          // TODO А надо ли тут допускать index ad open, если и так происходит запрос индекса?
          indexAdOpen     = Some(
            MIndexAdOpenQs(
              withBleBeaconAds = false,
            )
          ),
          adIds           = NonEmptyList( focAdId ),
        )
      },
      search = MAdsSearchReq(
        limit  = Some( ScQsUtil.adsPerReqLimit ),
        genOpt = Some( root.index.state.generation ),
        offset = Some( 0 ),
        textQuery = OptionUtil.maybeOpt(isSearchNodes) {
          Option( root.index.search.text.query )
            .filter(_.nonEmpty)
        }
      ),
      grid = Option.when( withStuff )(
        MScGridArgs(
          // Не надо плитку присылать, если demandLocTest: вопрос на экране не требует плитки.
          focAfterJump = OptionUtil.SomeBool.someTrue,
        )
      ),
      // Сразу запросить поиск по узлам, если панель поиска открыта.
      nodes = Option.when( isSearchNodes && withStuff )(
        MScNodesArgs(
          _searchNodes = true,
        )
      ),
    )

    // Check if index-request args are really changed URL qs:
    var dedupAgainst = (for {
      resp0 <- v0.resp
      // dedupViaScQs: ignore current index resp., if forcing flag defined.
      if switchCtx.dedupViaScQs
    } yield {
      resp0.scQs
    })
      .toList

    // For continious demand-index-testing, also deduplicate against already opened switchAsk QueryString args.
    for {
      inxAsk <- v0.state.switch.ask
      if switchCtx.demandLocTest
    }
      dedupAgainst ::= inxAsk.okAction.qs

    // Deduplicating requests:
    if (dedupAgainst contains[MScQs] args) {
      // Дубликат запроса. Бывает при запуске, когда jsRouter и wzFirst сыплят одинаковые RouteTo().
      noChange

    } else {
      // Надо делать запрос, обновив состояние:
      var fxAcc: List[Effect] = onChangeFxs
      fxAcc ::= Effect {
        api
          .pubApi(args)
          .transform { tryRes =>
            val r2 = HandleScApiResp(
              reqTimeStamp  = Some(ts),
              qs            = args,
              tryResp       = tryRes,
              reason        = reason,
              switchCtxOpt  = Some(switchCtx)
            )
            Success(r2)
          }
      }

      var valueModF = MScIndex.resp.modify(_.pending(ts))

      // Выставить в состояние, что запущен поиск узлов, чтобы не было дублирующихся запросов от контроллера панели.
      if (isSearchNodes) {
        valueModF = valueModF andThen MScIndex.search
          .andThen( MScSearch.geo )
          .andThen( MGeoTabS.found )
          .andThen( MNodesFoundS.req )
          .modify( _.pending(ts) )
      }

      val v2 = valueModF(v0)
      ah.updateMaybeSilentFx(silentUpdate)(v2, fxAcc.mergeEffects.get)
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен управления отображением боковых панелей выдачи.
    case m: SideBarOpenClose =>
      val v0 = value

      def __osStatusBarColorFxOpt(isPanelOpen: Boolean): Option[Effect] = {
        val mroot = rootRO.value
        val p = mroot.dev.platform
        Option.when( p.isCordova && p.isReady ) {
          IndexAh.osStatusBarColorFx( v0.scCss, usePanelColor = isPanelOpen )
        }
      }

      m.bar match {
        case MScSideBars.Search =>
          if (m.open contains[Boolean] v0.search.panel.opened) {
            // Ничего делать не надо - ничего не изменилось.
            noChange

          } else {
            // Действительно изменилось состояние отображения панели:
            val search_panel_open_LENS = IndexAh._inx_search_panel_opened_LENS
            var v2 = m.open
              .fold( search_panel_open_LENS.modify(!_) )( search_panel_open_LENS.replace )(v0)

            val isOpen2 = search_panel_open_LENS.get( v2 )

            // Не допускать открытости обеих панелей одновременно:
            if (isOpen2 && v2.menu.opened)
              v2 = (IndexAh._inx_menu_opened_LENS replace false)(v2)

            // Аккаумулятор сайд-эффектов.
            var fxAcc: Effect = ResetUrlRoute().toEffectPure

            // Требуется ли запускать инициализацию карты или списка найденных узлов? Да, если открытие на НЕинициализированной панели.
            if (isOpen2)
              for (fx <- SearchAh.maybeInitSearchPanel( v2.search ))
                fxAcc += fx

            for (fx <- __osStatusBarColorFxOpt(isOpen2))
              fxAcc += fx

            ah.updateMaybeSilentFx( m.silent )( v2, fxAcc )
          }

        case MScSideBars.Menu =>
          val menu_opened_LENS = IndexAh._inx_menu_opened_LENS
          if ( m.open contains[Boolean] menu_opened_LENS.get(v0) ) {
            noChange
          } else {
            var v2 = m.open.fold( menu_opened_LENS.modify(!_) )( menu_opened_LENS.replace )(v0)
            val isOpen2 = menu_opened_LENS.get( v2 )
            // Обновить URL.
            var fxAcc: Effect = ResetUrlRoute().toEffectPure

            for (fx <- __osStatusBarColorFxOpt(isOpen2))
              fxAcc += fx

            // Не допускать открытости обоих панелей одновременно.
            if (isOpen2 && v2.search.panel.opened)
              v2 = (IndexAh._inx_search_panel_opened_LENS replace false)(v2)

            ah.updateMaybeSilentFx( m.silent )(v2, fxAcc )
          }
      }


    // Клик по кнопке перехода на другой узел.
    case m @ GoToPrevIndexView =>
      val v0 = value
      v0.state.prevNodeOpt.fold(noChange) { prevNodeView =>
        // Сборка контекста переключения индекса с учётом данных из возможного сохранённого ранее в состоянии контекста.
        val indexQsArgs2 = MScIndexArgs(
          nodeId = prevNodeView.rcvrId,
        )
        val switchCtx2 = v0.state.viewCurrent
          .switchCtxOpt
          .getOrElse( MScSwitchCtx( indexQsArgs2 ) )
          .copy(
            indexQsArgs = indexQsArgs2,
            forceGeoLoc = (for {
              mgp <- prevNodeView.inxGeoPoint
            } yield {
              MGeoLoc(
                point = mgp,
                source = Some(MGeoLocSources.NodeInfo),
              )
            })
              .toList,
            showWelcome = false, // prevNodeView.rcvrId.nonEmpty,
            viewsAction = MScSwitchCtx.ViewsAction.POP,
          )

        // Запустить загрузку индекса - надо гео-точку подхватить.
        _getIndex(
          silentUpdate  = false,
          v0            = v0,
          // TODO m.asInstanceOf[IScIndexRespReason] - Ошибка в scalac-2.13.x. Потом - убрать asInstanceOf[].
          reason        = m.asInstanceOf[IScIndexRespReason],
          switchCtx     = switchCtx2
        )
      }


    // Клик по узлу в списке предлагаемых узлов:
    case m: IndexSwitchNodeClick =>
      val v0 = value

      val inx_state_switch_ask_LENS = IndexAh._inx_state_switch_ask_LENS
      inx_state_switch_ask_LENS
        .get(v0)
        .fold(noChange) { switchS0 =>
          def cleanedState = (inx_state_switch_ask_LENS replace None)(v0)

          m .nodeId
            .fold [Option[Either[ActionResult[M], MSc3IndexResp]]] {
              // Кнопка сокрытия диалога или иное сокрытие диалога (через клик по фоновому div'у).
              OptionUtil.maybe( v0.isFirstRun ) {
                // Начало запуска выдачи и отмена диалога. Надо как-то оказаться "на улице" - в геолокации, вне узлов.
                // Для этого надо повторить index-запрос, но с geoIntoRcvr=false.
                val fx = GetIndex(MScSwitchCtx(
                  indexQsArgs = MScIndexArgs(
                    geoIntoRcvr = false,
                    retUserLoc  = true,
                  ),
                )).toEffectPure
                val v2 = cleanedState
                Left( updated(v2, fx) )
              }
            } { nodeId =>
              // Выбран конкретный узел в списке.
              switchS0.nodesResp
                .nodesMap
                .get( nodeId )
                .map(m => Right(m.props))
            }
            .fold [ActionResult[M]] {
              // Не надо переключаться ни в какой узел. Просто сокрыть диалог.
              // Возможно, юзер не хочет уходить из текущего узла в новую определённую локацию.
              val v2 = cleanedState
              val fxOpt = switchS0.okAction
                .switchCtxOpt
                .flatMap(_.afterCancelSwitch)
              ah.updatedMaybeEffect( v2, fxOpt )

            } { resOrNodeFound =>
              resOrNodeFound.fold(
                identity,
                {nodeFound =>
                  val nodeFound2 = nodeFound.nodeId.fold {
                    // Ephemeral 404-node is selected. Need to reset geoPoint to qs loc env, so user will be properly placed.
                    switchS0.okAction.qs.common.locEnv.geoLoc
                      .headOption
                      .orElse {
                        // To use retUserLoc or current map result, if no geolocation was available during request (very first run index request).
                        v0.search.geo.mapInit.userLoc
                          .filter( _ =>  switchS0.okAction.qs.index.exists(_.retUserLoc) )
                      }
                      .fold( nodeFound ) { qsGeoLoc =>
                        (MSc3IndexResp.geoPoint replace Some(qsGeoLoc.point))(nodeFound)
                      }
                  }( _ => nodeFound )
                  // Надо сгенерить экшен переключения index'а в новое состояние. Все индексы включая выбранный уже есть в состоянии.
                  val actRes = IndexAh.indexUpdated(
                    i0      = cleanedState,
                    inx     = nodeFound2,
                    m       = switchS0.okAction,
                    mroot   = rootRO.value,
                  )
                  ah.updatedFrom( Some(actRes) )
                }
              )
            }
        }


    // Кто-то затребовал перерендерить css-стили выдачи. Скорее всего, размеры экрана изменились.
    case m: ScCssReBuild =>
      val v0 = value
      val scCssArgs = MScRoot.scCssArgsFrom( rootRO.value )
      if (v0.scCss.args != scCssArgs) {
        val scCss2 = ScCss( scCssArgs )
        val searchCss2 = SearchCss(
          args = (MSearchCssProps.screenInfo replace scCssArgs.screenInfo)(v0.search.geo.css.args),
        )

        val v2 = (
          MScIndex.scCss.replace( scCss2 ) andThen
          MScIndex.search
            .andThen( MScSearch.geo )
            .andThen( MGeoTabS.css )
            .replace( searchCss2 )
        )(v0)

        ah.updateMaybeSilent( m.silent )( v2 )

      } else {
        noChange
      }


    // Необходимо перезапросить индекс для текущего состояния карты
    case m: MapReIndex =>
      val v0 = value

      val outer_LENS = MScIndex.search
        .andThen( MScSearch.geo )

      val geoPointNextOpt = m.geoPoint
        .orElse {
          for {
            rcvrId <- m.rcvrId
            geoTab = outer_LENS.get(v0)
            nf     <- (
              geoTab.found.reqOpt #::
              // Или поискать в общей карте ресиверов:
              geoTab.data.rcvrsCache.toOption #::
              LazyList.empty
            )
              .iterator
              .flatMap( _.flatMap(_.resp.nodesMap.get(rcvrId)) )
              .nextOption()
            gp     <- scGeoUtil.nodePropsShapesToNodeGeoPoint( nf, v0.search.geo )
          } yield {
            gp
          }
        }

      val mmap = v0.search.geo.mapInit.state
      if (
        (m.rcvrId.nonEmpty && m.rcvrId ==* v0.state.rcvrId) ||
        (m.rcvrId.isEmpty && mmap.isCenterRealNearInit)
      ) {
        // Ничего как бы и не изменилось. Может карту просто переместить?
        (for {
          gp <- geoPointNextOpt
          if !(gp ~= mmap.center)
        } yield {
          val v2 = outer_LENS
            .andThen( MGeoTabS.mapInit )
            .andThen( MMapInitState.state )
            .modify( IndexAh.setMap(gp, m.rcvrId) )(v0)
          updated(v2)
        })
          .getOrElse( noChange )

      } else {

        var mapInitModF = MMapInitState.loader
          .replace( Some(mmap.center) )

        for (gp <- geoPointNextOpt) {
          // Перемещение в данную точку:
          mapInitModF = mapInitModF andThen MMapInitState.state
            .modify( IndexAh.setMap(gp, m.rcvrId) )
        }

        var geoTabModF = MGeoTabS.mapInit.modify( mapInitModF )

        // Обнулить id текущего тега.
        val geoTab_data_selTagIds_LENS = MGeoTabS.data
          .andThen( MGeoTabData.selTagIds )

        if (
          outer_LENS
            .andThen( geoTab_data_selTagIds_LENS )
            .get(v0)
            .nonEmpty
        ) {
          geoTabModF = geoTabModF andThen (geoTab_data_selTagIds_LENS replace Set.empty)
        }

        // Выставить новое состояние и запустить GetIndex.
        val v1 = (outer_LENS modify geoTabModF)( v0 )

        val switchCtx = MScSwitchCtx(
          indexQsArgs = MScIndexArgs(
            geoIntoRcvr   = m.rcvrId.nonEmpty,
            retUserLoc    = false,
            nodeId        = m.rcvrId,
          ),
          showWelcome = m.rcvrId.nonEmpty,
          indexMapReset = geoPointNextOpt.isEmpty,
        )

        _getIndex(
          silentUpdate  = false,
          v0            = v1,
          reason        = m,
          switchCtx     = switchCtx,
          // Если действительно что-то изменилось на карте - предложить обновление строки URL.
          onChangeFxs   = ResetUrlRoute().toEffectPure :: Nil,
        )
      }


    // Команда запроса и получения индекса выдачи с сервера для текущего состояния.
    case m: GetIndex =>
      _getIndex(
        silentUpdate  = true,
        v0            = value,
        reason        = m,
        switchCtx     = m.switchCtx,
      )


    // Перезагрузка текущего индекса.
    case m: ReGetIndex =>
      val v0 = value

      val switchCtx = MScSwitchCtx(
        indexQsArgs = MScIndexArgs(
          nodeId = v0.state.rcvrId,
          geoIntoRcvr = false,
          retUserLoc = false,
        ),
        focusedAdId = None,
        showWelcome = false,
        dedupViaScQs = false,
      )

      _getIndex(
        silentUpdate  = true,
        v0            = v0,
        reason        = m,
        switchCtx     = switchCtx,
      )


    // Undo state-changes, related to GetIndex.
    case GetIndexCancel =>
      val v0 = value
      if (v0.resp.isPending) {
        val v2 = MScIndex.resp.modify(_.unPending)(v0)
        updated( v2 )
      } else {
        noChange
      }


    // Отладка: обнуление текущего индекса.
    case UnIndex =>
      val v0 = value
      val v2 = (
        (MScIndex.resp replace Pot.empty) andThen
        (MScIndex.scCss
          .andThen(ScCss.args)
          .replace( MScCssArgs.from( None, rootRO.value.dev.screen.info ) )
        )
      )(v0)
      updated(v2)


  }

}

