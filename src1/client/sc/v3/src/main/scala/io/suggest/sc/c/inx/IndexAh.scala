package io.suggest.sc.c.inx

import diode._
import diode.data.{Pot, Ready}
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MScreenInfo
import io.suggest.geo.{MGeoLoc, MLocEnv}
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DiodeUtil.Implicits.ActionHandlerExt
import io.suggest.sc.ScConstants
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs}
import io.suggest.sc.m._
import io.suggest.sc.m.grid.{GridBlockClick, GridLoadAds}
import io.suggest.sc.m.inx._
import io.suggest.sc.m.search._
import io.suggest.sc.sc3._
import io.suggest.sc.styl.MScCssArgs
import io.suggest.sc.u.api.IScUniApi
import io.suggest.sc.v.ScCssFactory
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits.EffectsOps
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs}
import io.suggest.sc.c.grid.GridAh
import io.suggest.sc.c.search.SearchAh
import io.suggest.sc.v.search.SearchCss
import japgolly.univeq._
import scalaz.NonEmptyList

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 18:39
  * Description: Контроллер index'а выдачи.
  */

class IndexRah(scCssFactory: ScCssFactory )
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[IScIndexRespReason]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    Some( ctx.value0.index.resp )
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): MScRoot = {
    val i0 = ctx.value0.index
    LOG.error( ErrorMsgs.GET_NODE_INDEX_FAILED, ex, msg = ctx.m )
    var i2 = i0.withResp(
      i0.resp.fail(ex)
    )
    if (i2.search.geo.mapInit.loader.nonEmpty) {
      i2 = i2.withSearch(
        i2.search.resetMapLoader
      )
    }
    ctx.value0.withIndex( i2 )
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.Index
  }


  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val v0 = ctx.value0
    val i0 = v0.index

    // Если нод много, то надо плашку рисовать на экране.
    val resp = ra.search.get
    val isMultiNodeResp = resp.hasManyNodes

    // Сравнивать полученный index с текущим состоянием. Может быть ничего сохранять не надо?
    if (
      !isMultiNodeResp &&
      resp.results.exists { node =>
        v0.index.resp.exists { rn =>
          MSc3IndexResp.isLookingSame(rn, node.props)
        }
      }
    ) {
      var i1 = i0

      // Убрать возможный pending из resp:
      if (i1.resp.isPending) {
        i1 = i1.withResp(
          i1.resp
            .ready( i1.resp.get )
        )
      }
      // Убрать крутилку-preloader с гео-карты:
      if (i1.search.geo.mapInit.loader.nonEmpty)
        i1 = i1.withSearch( i1.search.resetMapLoader )
      if (i1.state.switchAsk.nonEmpty) {
        i1 = i1.withState(
          i1.state.withSwitchAsk( None )
        )
      }

      val v2 = v0.withIndex( i1 )

      // Запустить эффект обновления плитки, если плитка не пришла автоматом.
      val gridLoadFxOpt = OptionUtil.maybe(
        !ctx.m.tryResp
          .toOption
          .exists(_.respActionTypes contains MScRespActionTypes.AdsTile)
      ) {
        GridLoadAds(clean = true, ignorePending = true)
          .toEffectPure
      }

      ActionResult( Some(v2), gridLoadFxOpt )

    } else if (isMultiNodeResp || ctx.m.switchCtxOpt.exists(_.demandLocTest)) {
      // Это тест переключения выдачи в новое местоположение, и узел явно изменился.
      // Вместо переключения узла, надо спросить юзера, можно ли переходить полученный узел:
      val switchAskState = MInxSwitchAskS(
        okAction  = ctx.m,
        nodesResp = resp,
        searchCss = OptionUtil.maybe(isMultiNodeResp) {
          val cssArgs = MSearchCssProps(
            req         = Ready( MSearchRespInfo(resp = resp.results) ),
            screenInfo  = ctx.value0.dev.screen.info
          )
          SearchCss( cssArgs )
        },
      )

      val v2 = v0.withIndex {
        v0.index.withState(
          v0.index.state
            .withSwitchAsk( Some(switchAskState) )
        )
      }

      ActionResult.ModelUpdate(v2)

    } else {
      // Индекс изменился, значит заливаем новый индекс в состояние:
      val actRes1 = indexUpdated(
        i0        = v0.index,
        inx       = resp.results.head.props,
        m         = ctx.m,
        mscreen   = v0.dev.screen.info
      )
      ActionResult(
        for (inx2 <- actRes1.newModelOpt) yield {
          v0.withIndex(inx2)
        },
        actRes1.effectOpt
      )
    }
  }


  /** Непосредственное обновление индекса.
    *
    * @param inx Новый индекс.
    * @return ActionResult.
    */
  def indexUpdated(i0: MScIndex, inx: MSc3IndexResp, m: HandleScApiResp, mscreen: MScreenInfo): ActionResult[MScIndex] = {
    // Сайд-эффекты закидываются в этот аккамулятор:
    var fxsAcc = List.empty[Effect]

    val nextIndexView = MIndexView(
      rcvrId    = inx.nodeId,
      // Если nodeId не задан, то взять гео-точку из qs
      inxGeoPoint = OptionUtil.maybeOpt( inx.nodeId.isEmpty ) {
        // Не используем текущее значение карты, т.к. карта тоже могла измениться:
        m.qs.common.locEnv.geoLocOpt
          .map(_.point)
      },
      name = inx.name
    )

    var i1 = i0.copy(
      resp = i0.resp.ready(inx),
      state = i0.state.copy(
        switchAsk = None,
        // Если фокусировка, то разрешить шаг наверх:
        views = if ( m.reason.isInstanceOf[GridBlockClick] ) {
          //println("append: " + nextIndexView + " :: " + i0.state.views)
          nextIndexView <:: i0.state.views
        } else {
          //println("replace: " + nextIndexView + " " + ctx.m.reason)
          NonEmptyList( nextIndexView )
        }
      ),
      search = {
        var s0 = i0.search

        // Выставить полученную с сервера геоточку как текущую.
        // TODO Не сбрасывать точку, если index не изменился.
        s0 = inx.geoPoint
          .filter { mgp =>
            !(i0.search.geo.mapInit.state.center ~= mgp)
          }
          .fold(s0) { mgp =>
            s0.withGeo(
              s0.geo.withMapInit(
                s0.geo.mapInit.withState(
                  s0.geo.mapInit.state.copy(
                    centerInit = mgp,
                    centerReal = None,
                    // Увеличить зум, чтобы приблизить.
                    zoom = 15
                  )
                )
              )
            )
          }

        // Если возвращена userGeoLoc с сервера, которая запрашивалась и до сих пор она нужна, то её выставить в состояние.
        for {
          _ <- inx.userGeoLoc
          if s0.geo.mapInit.userLoc.isEmpty
        } {
          s0 = s0.withGeo(
            s0.geo.withMapInit(
              s0.geo.mapInit
                .withUserLoc( inx.userGeoLoc )
            )
          )
        }

        // Возможный сброс состояния тегов
        s0 = s0.maybeResetNodesFound

        // Если заход в узел с карты, то надо скрыть search-панель.
        if (s0.panel.opened && inx.welcome.nonEmpty) {
          s0 = s0.withPanel(
            s0.panel.withOpened( false )
          )
        }

        // Сбросить флаг mapInit.loader, если он выставлен.
        if (s0.geo.mapInit.loader.nonEmpty)
          s0 = s0.resetMapLoader

        // Сбросить текст поиска, чтобы теги отобразились на экране.
        if (s0.text.query.nonEmpty)
          fxsAcc ::= SearchTextChanged("").toEffectPure

        s0
      }
    )

    val respActionTypes = m.tryResp.get.respActionTypes
    // Если панель поиск видна, то запустить поиск узлов в фоне.
    if (i1.search.panel.opened && !(respActionTypes contains MScRespActionTypes.SearchNodes))
      fxsAcc ::= SearchAh.reDoSearchFx( ignorePending = false )

    // Возможно, нужно организовать обновление URL в связи с обновлением состояния узла.
    fxsAcc ::= ResetUrlRoute.toEffectPure

    // Нужно огранизовать инициализацию плитки карточек. Для этого нужен эффект:
    if ( !(respActionTypes contains MScRespActionTypes.AdsTile) ) {
      fxsAcc ::= GridLoadAds(clean = true, ignorePending = true)
        .toEffectPure
    }

    // Инициализация приветствия. Подготовить состояние welcome.
    val mWcSFutOpt = for {
      resp <- i1.resp.toOption
      if !i1.resp.isFailed      // Всякие FailingStale содержат старый ответ и новую ошибку, их надо отсеять.
      wcInfo2 <- resp.welcome
      // Не надо отображать текущее приветствие повторно:
      if !i0.resp.exists(_.welcome contains wcInfo2)
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
    i1 = i1.withWelcome( mWcSFutOpt.map(_._2) )

    // Нужно отребилдить ScCss, но только если что-то реально изменилось.
    val scCssArgs2 = MScCssArgs.from(i1.resp, mscreen)
    if (scCssArgs2 != i1.scCss.args) {
      // Изменились аргументы. Пора отребилдить ScCss.
      i1 = i1.withScCss(
        scCssFactory.mkScCss( scCssArgs2 )
      )
    }

    // Объединить эффекты плитки и приветствия воедино:
    for (mwc <- mWcSFutOpt)
      fxsAcc ::= mwc._1

    val fxOpt = fxsAcc.mergeEffects
    ActionResult( Some(i1), fxOpt )
  }

}


/** diode-контроллер. */
class IndexAh[M](
                  api           : IScUniApi,
                  modelRW       : ModelRW[M, MScIndex],
                  rootRO        : ModelRO[MScRoot],
                  indexRah      : IndexRah,
                  scCssFactory  : ScCssFactory,
                )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Непосредственный запуск получения индекса с сервера.
    *
    * @param silentUpdate Не рендерить на экране изменений?
    * @param v0 Исходныое значение MScIndex.
    * @param reason Экшен-причина, приведший к запуску запроса.
    * @param switchCtx Данные контекст обновления всей выдачи.
    * @return ActionResult.
    */
  private def _getIndex(silentUpdate: Boolean, v0: MScIndex,
                        reason: IScIndexRespReason, switchCtx: MScSwitchCtx): ActionResult[M] = {
    val ts = System.currentTimeMillis()
    val root = rootRO.value

    val isSearchNodes = root.index.search.panel.opened
    val searchArgs = MAdsSearchReq(
      limit  = Some( GridAh.adsPerReqLimit ),
      genOpt = Some( root.index.state.generation ),
      offset = Some( 0 ),
      textQuery = OptionUtil.maybeOpt(isSearchNodes) {
        Option( root.index.search.text.query )
          .filter(_.nonEmpty)
      }
    )

    val fx = Effect {
      val withAdditional = !switchCtx.demandLocTest
      val args = MScQs(
        common = MScCommonQs(
          apiVsn = root.internals.conf.apiVsn,
          // Когда уже задан id-ресивера, не надо слать на сервер маячки и географию.
          locEnv = {
            // Слать на сервер координаты с карты или с gps, если идёт определение местоположения.
            if (switchCtx.demandLocTest) root.locEnvUser
            else if (switchCtx.forceGeoLoc.nonEmpty) MLocEnv(switchCtx.forceGeoLoc, root.locEnvBleBeacons)
            else if (switchCtx.indexQsArgs.nodeId.isEmpty) root.locEnvMap
            else MLocEnv.empty
          },
          screen = Some( root.dev.screen.info.screen ),
          // Не надо плитку присылать, если demandLocTest: вопрос на экране не требует плитки.
          searchGridAds = OptionUtil.maybeTrue( withAdditional ),
          // Сразу запросить поиск по узлам, если панель поиска открыта.
          searchNodes = OptionUtil.maybeTrue( isSearchNodes && withAdditional ),
        ),
        index = Some( switchCtx.indexQsArgs ),
        // Фокусироваться надо при запуске. Для этого следует получать всё из reason, а не из состояния.
        foc = for {
          focAdId <- switchCtx.focusedAdId
          if withAdditional
        } yield {
          MScFocusArgs(
            focIndexAllowed = true,
            lookupMode      = None,
            lookupAdId      = focAdId
          )
        },
        search = searchArgs
      )

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

    var v2 = v0.withResp(
      v0.resp.pending(ts)
    )

    // Выставить в состояние, что запущен поиск узлов, чтобы не было дублирующихся запросов от контроллера панели.
    if (isSearchNodes) {
      v2 = v2.withSearch(
        v2.search.withGeo(
          v2.search.geo.withFound(
            v2.search.geo.found.withReqWithArgs(
              req = v2.search.geo.found.req.pending(ts),
              reqSearchArgs = Some(searchArgs)
            )
          )
        )
      )
    }

    ah.updateMaybeSilentFx(silentUpdate)(v2, fx)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен управления отображением боковых панелей выдачи.
    case m: SideBarOpenClose =>
      val v0 = value
      m.bar match {
        case MScSideBars.Search =>
          if (v0.search.panel.opened ==* m.open) {
            // Ничего делать не надо - ничего не изменилось.
            noChange

          } else {
            // Действительно изменилось состояние отображения панели:
            var v2 = v0.withSearch(
              v0.search.withPanel(
                v0.search.panel
                  .withOpened( m.open )
              )
            )

            // Не допускать открытости обеих панелей одновременно:
            if (m.open && v2.menu.opened) {
              v2 = v2.withMenu(
                v2.menu.withOpened(false)
              )
            }

            // Аккаумулятор сайд-эффектов.
            val routeFx = ResetUrlRoute.toEffectPure

            // Требуется ли запускать инициализацию карты или списка найденных узлов? Да, если открытие на НЕинициализированной панели.
            val fxOpt = OptionUtil.maybeOpt(m.open) {
              SearchAh.maybeInitSearchPanel(v2.search)
            }

            // Объеденить эффекты:
            val finalFx = (routeFx :: fxOpt.toList)
              .mergeEffects
              .get

            updated(v2, finalFx)
          }

        case MScSideBars.Menu =>
          if (v0.menu.opened !=* m.open) {
            var v2 = v0.withMenu(
              v0.menu.withOpened( m.open )
            )
            // Обновить URL.
            val fx = ResetUrlRoute.toEffectPure

            // Не допускать открытости обоих панелей одновременно.
            if (m.open && v2.search.panel.opened) {
              v2 = v2.withSearch(
                v2.search.withPanel(
                  v2.search.panel
                    .withOpened( false )
                )
              )
            }

            updated( v2, fx )
          } else {
            noChange
          }
      }


    // Клик по кнопке перехода на другой узел.
    case m @ GoToPrevIndexView =>
      val v0 = value
      v0.state.prevNodeOpt.fold(noChange) { prevNodeView =>
        // Контекст переключения.
        val switchCtx = MScSwitchCtx(
          indexQsArgs = MScIndexArgs(
            nodeId      = prevNodeView.rcvrId,
            withWelcome = prevNodeView.rcvrId.nonEmpty,
          ),
          forceGeoLoc = for (mgp <- prevNodeView.inxGeoPoint) yield {
            MGeoLoc(point = mgp)
          }
        )

        // Запустить загрузку индекса - надо гео-точку подхватить.
        _getIndex(
          silentUpdate  = false,
          v0            = v0,
          reason        = m,
          switchCtx     = switchCtx
        )
      }


    // Клик по узлу в списке предлагаемых узлов:
    case m: NodeRowClick if value.state.switchAsk.nonEmpty =>
      val v0 = value
      // Надо сгенерить экшен переключения index'а в новое состояние. Все индексы включая выбранный уже есть в состоянии.
      val actResOpt = for {
        switchS <- v0.state.switchAsk
        inx0    <- switchS.nodesResp.results
          .find(_.props.nameOrIdOrEmpty ==* m.nodeId)
      } yield {
        indexRah.indexUpdated(
          i0      = v0.withState(
            v0.state.withSwitchAsk( None )
          ),
          inx     = inx0.props,
          m       = switchS.okAction,
          mscreen = rootRO.value.dev.screen.info,
        )
      }
      ah.updatedFrom( actResOpt )


    // Кто-то затребовал перерендерить css-стили выдачи. Скорее всего, размеры экрана изменились.
    case ScCssReBuild =>
      val v0 = value
      val scCssArgs = MScRoot.scCssArgsFrom( rootRO.value )
      if (v0.scCss.args != scCssArgs) {
        val scCss2 = scCssFactory.mkScCss( scCssArgs )
        val searchCss2 = SearchCss( v0.search.geo.css.args.withScreenInfo(scCssArgs.screenInfo) )
        val v2 = v0
          .withScCss( scCss2 )
          .withSearch(
            v0.search.withGeo(
              v0.search.geo
                .withCss( searchCss2 )
            )
          )
        val rszMapFx = for (lmap <- v0.search.geo.data.lmap) yield SearchAh.mapResizeFx(lmap)
        ah.updatedMaybeEffect(v2, rszMapFx)
      } else {
        noChange
      }


    // Необходимо перезапросить индекс для текущего состояния карты
    case m: MapReIndex =>
      val v0 = value
      if (
        (m.rcvrId.nonEmpty && m.rcvrId ==* v0.state.rcvrId) ||
        (m.rcvrId.isEmpty && v0.search.geo.mapInit.state.isCenterRealNearInit)
      ) {
        // Ничего как бы и не изменилось.
        noChange
      } else {
        // Выставить новое состояние и запустить GetIndex.
        val v1 = v0
          .withSearch(
            v0.search.withGeo(
              v0.search.geo.withMapInit(
                v0.search.geo.mapInit.withLoader(
                  Some( v0.search.geo.mapInit.state.center )
                )
              )
            )
          )

        val switchCtx = MScSwitchCtx(
          indexQsArgs = MScIndexArgs(
            withWelcome   = m.rcvrId.nonEmpty,
            geoIntoRcvr   = m.rcvrId.nonEmpty,
            retUserLoc    = false,
            nodeId        = m.rcvrId,
          )
        )

        _getIndex(
          silentUpdate  = false,
          v0            = v1,
          reason        = m,
          switchCtx     = switchCtx,
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


    // Юзер подтверждает переход в новую локацию.
      // TODO Эта кнопка актуальная, если несколько индексов?
    case ApproveIndexSwitch =>
      // Надо выпустить на свободу экшен внутри switch-состояния, обнулив switch-состояние.
      val v0 = value
      v0.state.switchAsk.fold(noChange) { switchS =>
        val fx = switchS.okAction
          // Надо сбросить switch-состояние, иначе диалог зациклится без видимых результатов.
          .withSwitchCtxOpt( None )
          .toEffectPure

        // И убрать сам диалог с экрана:
        val v2 = v0.withState(
          v0.state.withSwitchAsk( None )
        )

        updated( v2, fx )
      }


    // Юзер не хочет уходить из текущего узла в новую определённую локацию.
    case CancelIndexSwitch =>
      val v0 = value
      v0.state.switchAsk.fold(noChange) { switchS =>
        val v1 = v0.withState(
          v0.state.withSwitchAsk( None )
        )
        if (v0.resp.isEmpty) {
          // Если нет открытого узла (выдача скрыта), то надо выбрать первый узел из списка.
          val fx = NodeRowClick(
            nodeId = switchS.nodesResp.results.head.props.nameOrIdOrEmpty
          )
            .toEffectPure
          effectOnly(fx)
        } else {
          // Есть открытый узел. Просто скрыть диалог.
          updated( v1 )
        }
      }

  }

}

