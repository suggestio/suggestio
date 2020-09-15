package io.suggest.sc

import diode.{Effect, FastEq, ModelRW}
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.ble.beaconer.{BleBeaconerAh, BtOnOff, MBeaconerOpts, MBeaconerS}
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.cordova.background.fetch.CdvBgFetchAh
import io.suggest.cordova.background.mode.CordovaBgModeAh
import io.suggest.daemon.{BgModeDaemonInit, HtmlBgTimerAh, MDaemonDescr, MDaemonInitOpts, MDaemonStates}
import io.suggest.dev.MScreen.MScreenFastEq
import io.suggest.dev.MScreenInfo.MScreenInfoFastEq
import io.suggest.dev.{JsScreenUtil, MPlatformS, MScreenInfo}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdConf
import io.suggest.jd.render.c.JdAh
import io.suggest.jd.render.u.JdUtil
import io.suggest.maps.c.MapCommonAh
import io.suggest.maps.m.MMapS
import io.suggest.maps.m.MMapS.MMapSFastEq4Map
import io.suggest.maps.u.AdvRcvrsMapApiHttpViaUrl
import io.suggest.msg._
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.os.notify.api.cnl.CordovaLocalNotificationAh
import io.suggest.routes.routes
import io.suggest.sc.ads.MScNodeMatchInfo
import io.suggest.sc.c.dev.{GeoLocAh, OnLineAh, PlatformAh, ScreenAh}
import io.suggest.sc.c._
import io.suggest.sc.c.dia.{ScErrorDiaAh, ScLoginDiaAh, ScNodesDiaAh, ScSettingsDiaAh, WzFirstDiaAh}
import io.suggest.sc.c.grid.{GridAh, GridFocusRespHandler, GridRespHandler}
import io.suggest.sc.c.inx.{ConfUpdateRah, IndexAh, IndexRah, ScConfAh, WelcomeAh}
import io.suggest.sc.c.jsrr.JsRouterInitAh
import io.suggest.sc.c.menu.DlAppAh
import io.suggest.sc.c.search._
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs}
import io.suggest.sc.m._
import io.suggest.sc.m.boot.MScBoot.MScBootFastEq
import io.suggest.sc.m.boot.{Boot, MBootServiceIds, MSpaRouterState}
import io.suggest.sc.m.dev.{MScDev, MScOsNotifyS, MScScreenS}
import io.suggest.sc.m.dia.MScDialogs
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.grid.{GridAfterUpdate, GridLoadAds, MGridCoreS, MGridS}
import io.suggest.sc.m.in.{MInternalInfo, MScDaemon, MScInternals}
import io.suggest.sc.m.inx.{MScIndex, MScIndexState, MScSwitchCtx}
import io.suggest.sc.m.menu.{MDlAppDia, MMenuS}
import io.suggest.sc.m.search.MGeoTabS.MGeoTabSFastEq
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.{MSc3Conf, MSc3Init}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sc.u.api.{IScAppApi, IScUniApi}
import io.suggest.sc.v.search.SearchCss
import io.suggest.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2._
import io.suggest.spa.{DAction, DoNothingActionProcessor, FastEqUtil, OptFastEq}
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.CircuitUtil._
import org.scalajs.dom
import io.suggest.event.DomEvents
import io.suggest.id.login.LoginFormCircuit
import io.suggest.id.login.c.IIdentApi
import io.suggest.id.login.c.session.{LogOutAh, LoginSessionAh}
import io.suggest.id.login.m.session.{MLogOutDia, MLoginSessionS}
import io.suggest.lk.c.{CsrfTokenAh, ICsrfTokenApi}
import io.suggest.lk.m.LoginSessionRestore
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.r.plat.PlatformCssStatic
import io.suggest.os.notify.{CloseNotify, NotifyStartStop}
import io.suggest.os.notify.api.html5.{Html5NotificationApiAdp, Html5NotificationUtil}
import io.suggest.sc.c.in.{BootAh, ScDaemonAh}
import io.suggest.sc.m.inx.save.{MIndexesRecent, MIndexesRecentOuter}
import io.suggest.sc.m.styl.MScCssArgs
import io.suggest.sc.v.styl.ScCss
import io.suggest.sc.v.toast.ScNotifications
import io.suggest.ueq.UnivEqUtil._

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:00
  * Description: Main circuit новой выдачи. Отрабатывает весь интерфейс выдачи v3.
  */
class Sc3Circuit(
                  // Явные аргументы:
                  routerState               : MSpaRouterState,
                  getLoginFormCircuit       : () => LoginFormCircuit,
                  getNodesFormCircuit       : () => LkNodesFormCircuit,
                  // Автоматические DI-аргументы:
                  sc3UniApi                 : IScUniApi,
                  scAppApi                  : IScAppApi,
                  csrfTokenApi              : ICsrfTokenApi,
                  mkLogOutAh                : ModelRW[MScRoot, Option[MLogOutDia]] => LogOutAh[MScRoot],
                )
  extends CircuitLog[MScRoot]
  with ReactConnector[MScRoot]
{ circuit =>

  import MScIndex.MScIndexFastEq
  import m.in.MScInternals.MScInternalsFastEq
  import MGridS.MGridSFastEq
  import MScDev.MScDevFastEq
  import MScScreenS.MScScreenSFastEq
  import m.dev.MScGeoLoc.MScGeoLocFastEq
  import m.inx.MWelcomeState.MWelcomeStateFastEq

  import MScSearch.MScSearchFastEq
  import m.search.MScSearchText.MScSearchTextFastEq
  import MScRoot.MScRootFastEq
  import MMapInitState.MMapInitStateFastEq
  import io.suggest.sc.m.dia.MScDialogs.MScDialogsFastEq
  import io.suggest.sc.m.dia.first.MWzFirstOuterS.MWzFirstOuterSFastEq

  import MBeaconerS.MBeaconerSFastEq

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  override protected def initialModel: MScRoot = {
    // Сначала надо подготовить конфиг, прочитав его со страницы (если он там есть).
    val scInit = Sc3ConfUtil.getFreshestInit()
      .getOrElse {
        val emsg = ErrorMsgs.GRID_CONFIGURATION_INVALID
        logger.error( emsg )
        // TODO Отработать отсутствие конфига в html-странице.
        throw new NoSuchElementException( emsg )
      }

    val mscreen = JsScreenUtil.getScreen()
    val mplatform = PlatformAh.platformInit(this)

    val screenInfo = MScreenInfo(
      screen        = mscreen,
      //unsafeOffsets = HwScreenUtil.etScreenUnsafeAreas( mscreen ),
    )

    val scIndexResp = Pot.empty[MSc3IndexResp]

    // random seed изначально отсутствует в конфиге.
    val (conf2, gen2) = scInit.conf.gen.fold {
      val generation2 = System.currentTimeMillis()
      val scInit2 = MSc3Init.conf.modify { conf0 =>
        val conf1 = MSc3Conf.gen
          .set( Some(generation2) )(conf0)
        Sc3ConfUtil.prepareSave( conf1 )
      }(scInit)

      Sc3ConfUtil.saveInitIfPossible( scInit2 )

      scInit2.conf -> generation2
    } { gen =>
      scInit.conf -> gen
    }

    val searchCssEmpty = SearchCss( MSearchCssProps(
      screenInfo = screenInfo
    ))

    MScRoot(
      dev = MScDev(
        screen = MScScreenS(
          info = screenInfo,
        ),
        platform = mplatform,
        platformCss   = PlatformCssStatic(
          isRenderIos = mplatform.isUseIosStyles,
        ),
      ),
      index = MScIndex(
        resp = scIndexResp,
        search = MScSearch(
          geo = MGeoTabS(
            mapInit = MMapInitState(
              state = MMapS(scInit.mapProps)
            ),
            css = searchCssEmpty,
          ),
        ),
        scCss = ScCss(
          MScCssArgs.from(scIndexResp, screenInfo)
        ),
        state = MScIndexState(
          generation = gen2,
        )
      ),
      grid = {
        val (gridColsCount, gridSzMult) = GridAh.fullGridConf(mscreen.wh)
        val jdConf = MJdConf(
          isEdit            = false,
          gridColumnsCount  = gridColsCount,
          szMult            = gridSzMult
        )
        MGridS(
          core = MGridCoreS(
            jdConf    = jdConf,
            jdRuntime = JdUtil.mkRuntime(jdConf).result,
          )
        )
      },
      internals = MScInternals(
        conf = conf2,
        info = MInternalInfo(
          indexesRecents = MIndexesRecentOuter(
            searchCss = searchCssEmpty,
            saved = Pot.empty[MIndexesRecent],
          ),
        ),
      ),
    )
  }

  // Сразу подписаться на глобальные ошибки:
  {
    import io.suggest.sjs.common.vm.evtg.EventTargetVm._
    dom.window.addEventListener4s( DomEvents.ERROR ) { e: dom.ErrorEvent =>
      def _s(f: => js.UndefOr[_]): String =
        Try(f.fold("")(_.toString)) getOrElse ""

      val msg = (_s(e.messageU), _s(e.filenameU), (_s(e.linenoU), _s(e.colnoU)) )
      val errCode = MsgCodes.`Malfunction`

      logger.error(
        errCode,
        msg = (msg, _s(e.error.map(_.name)), _s(e.error.flatMap(_.message)), _s(e.error.flatMap(_.stack)) ),
      )

      val action = SetErrorState(
        MScErrorDia(
          messageCode = errCode,
          hint        = Some( msg.toString ),
        )
      )
      this.runEffectAction( action )
    }
  }

  // Кэш zoom'ов модели:
  private[sc] val rootRW          = zoomRW(identity) { (_, new2) => new2 } ( MScRootFastEq )

  private[sc] val internalsRW     = mkLensRootZoomRW(this, MScRoot.internals)( MScInternalsFastEq )
  private[sc] val internalsInfoRW = mkLensZoomRW( internalsRW, MScInternals.info )
  private[sc] val csrfTokenRW     = mkLensZoomRW( internalsInfoRW, MInternalInfo.csrfToken )
  private[sc] val currRouteRW     = mkLensZoomRW( internalsInfoRW, MInternalInfo.currRoute )

  private[sc] val indexRW         = mkLensRootZoomRW(this, MScRoot.index)(MScIndexFastEq)
  private[sc] val titlePartsRO    = rootRW.zoom [List[String]] { mroot =>
    var acc = List.empty[String]

    // Заголовок узла.
    for {
      inxResp <- mroot.index.resp
      nodeName <- inxResp.name
    } {
      acc ::= nodeName
    }

    // Заголовок карточки.
    for {
      scAd <- mroot.grid.core.ads
        .iterator
        .flatten
      foc <- scAd.focused
      focAdTitle <- foc.title
    } {
      acc ::= focAdTitle
    }

    acc
  }( FastEq.ValueEq )

  private val indexWelcomeRW      = mkLensZoomRW(indexRW, MScIndex.welcome)( OptFastEq.Wrapped(MWelcomeStateFastEq) )
  private[sc] def scCssRO         = mkLensZoomRO(indexRW, MScIndex.scCss)

  private val searchRW            = mkLensZoomRW(indexRW, MScIndex.search)( MScSearchFastEq )
  private val geoTabRW            = mkLensZoomRW(searchRW, MScSearch.geo)( MGeoTabSFastEq )

  private val mapInitRW           = mkLensZoomRW(geoTabRW, MGeoTabS.mapInit)( MMapInitStateFastEq )
  private val mmapsRW             = mkLensZoomRW(mapInitRW, MMapInitState.state)( MMapSFastEq4Map )
  private val searchTextRW        = mkLensZoomRW(searchRW, MScSearch.text)( MScSearchTextFastEq )
  private[sc] val geoTabDataRW    = mkLensZoomRW(geoTabRW, MGeoTabS.data)( MGeoTabData.MGeoTabDataFastEq )
  private val mapDelayRW          = mkLensZoomRW(geoTabDataRW, MGeoTabData.delay)( OptFastEq.Wrapped(MMapDelay.MMapDelayFastEq) )

  private val gridRW              = mkLensRootZoomRW(this, MScRoot.grid)( MGridSFastEq )
  private val gridCoreRW          = mkLensZoomRW( gridRW, MGridS.core )( MGridCoreS.MGridCoreSFastEq )
  private val jdRuntimeRW         = mkLensZoomRW( gridCoreRW, MGridCoreS.jdRuntime )( FastEqUtil.AnyRefFastEq )

  private[sc] val devRW           = mkLensRootZoomRW(this, MScRoot.dev)( MScDevFastEq )
  private val scScreenRW          = mkLensZoomRW(devRW, MScDev.screen)( MScScreenSFastEq )
  private val scGeoLocRW          = mkLensZoomRW(devRW, MScDev.geoLoc)( MScGeoLocFastEq )
  private val onLineRW            = mkLensZoomRW(devRW, MScDev.onLine)

  private val confRW              = mkLensZoomRW(internalsRW, MScInternals.conf)( MSc3Conf.MSc3ConfFastEq )
  private val rcvrsMapUrlRO       = mkLensZoomRO(confRW, MSc3Conf.rcvrsMapUrl)( FastEq.AnyRefEq )

  private[sc] val platformRW      = mkLensZoomRW(devRW, MScDev.platform)( MPlatformS.MPlatformSFastEq )
  private[sc] def platformCssRO   = mkLensZoomRO(devRW, MScDev.platformCss)

  private[sc] val beaconerRW      = mkLensZoomRW(devRW, MScDev.beaconer)( MBeaconerSFastEq )
  private[sc] val beaconsNearbyRO = mkLensZoomRO( beaconerRW, MBeaconerS.nearbyReport )

  private val dialogsRW           = mkLensRootZoomRW(this, MScRoot.dialogs )( MScDialogsFastEq )
  private[sc] val firstRunDiaRW   = mkLensZoomRW(dialogsRW, MScDialogs.first)( MWzFirstOuterSFastEq )
  private[sc] val scLoginRW       = mkLensZoomRW(dialogsRW, MScDialogs.login)
  private val scNodesRW           = mkLensZoomRW(dialogsRW, MScDialogs.nodes)

  private val bootRW              = mkLensZoomRW(internalsRW, MScInternals.boot)( MScBootFastEq )
  private[sc] val jsRouterRW      = mkLensZoomRW(internalsRW, MScInternals.jsRouter )( FastEqUtil.AnyRefFastEq )
  private val scErrorDiaRW        = mkLensZoomRW(dialogsRW, MScDialogs.error)( OptFastEq.Wrapped(MScErrorDia.MScErrorDiaFastEq) )

  private val menuRW              = mkLensZoomRW( indexRW, MScIndex.menu )( MMenuS.MMenuSFastEq )
  private val dlAppDiaRW          = mkLensZoomRW( menuRW, MMenuS.dlApp )( MDlAppDia.MDlAppDiaFeq )

  private[sc] val inxStateRO      = mkLensZoomRO( indexRW, MScIndex.state )

  private val screenInfoRO        = mkLensZoomRO(scScreenRW, MScScreenS.info)( MScreenInfoFastEq )
  private val screenRO            = mkLensZoomRO(screenInfoRO, MScreenInfo.screen)( MScreenFastEq )

  private val osNotifyRW          = mkLensZoomRW(devRW, MScDev.osNotify)( MScOsNotifyS.MScOsNotifyFeq )
  val loggedInRO                  = indexRW.zoom(_.isLoggedIn)

  private lazy val daemonRW       = mkLensZoomRW( internalsRW, MScInternals.daemon )
  private[sc] val loginSessionRW  = mkLensZoomRW( internalsRW, MScInternals.login )
  private[sc] val logOutRW        = mkLensZoomRW( loginSessionRW, MLoginSessionS.logout )


  // notifications
  private val scNotifications = new ScNotifications(
    rootRO = rootRW,
  )

  /** Списки обработчиков ответов ScUniApi с сервера и resp-action в этих ответах. */
  val (respHandlers, respActionHandlers) = {
    // Часть модулей является универсальной, поэтому шарим хвост списка между обоими списками:
    val mixed = List[IRespWithActionHandler](
      new GridRespHandler(
        isDoOsNotify = rootRW.zoom { mroot =>
          // Разрешается делать нотификацию уровня ОС только если:
          // 1. Есть разрешение на нотификации.
          (mroot.dev.osNotify.hasPermission contains true) //&&
          // TODO 2. Приложение скрыто, и требует привлечения внимания, и запрос был в фоне.
          //!mroot.dev.platform.isUsingNow
        },
        scNotifications = scNotifications,
      ),
      new GridFocusRespHandler,
      new IndexRah,
      new NodesSearchRah( screenInfoRO ),
    )

    val rahs: List[IRespActionHandler] =
      new ConfUpdateRah ::
      mixed

    val rhs: List[IRespHandler] =
      mixed

    (rhs, rahs)
  }


  // Action-Handler'ы

  // хвостовой контроллер -- в самом конце, когда остальные отказались обрабатывать сообщение.
  private val tailAh = new TailAh(
    modelRW               = rootRW,
    routerCtl             = routerState.routerCtl,
    scRespHandlers        = respHandlers,
    scRespActionHandlers  = respActionHandlers,
  )

  private val geoTabAh = new GeoTabAh(
    modelRW         = geoTabRW,
    api             = sc3UniApi,
    scRootRO        = rootRW,
    rcvrsMapApi     = advRcvrsMapApi,
    screenInfoRO    = screenInfoRO,
    rcvrMapArgsRO   = rcvrsMapUrlRO,
  )

  private val indexAh = new IndexAh(
    api     = sc3UniApi,
    modelRW = indexRW,
    rootRO  = rootRW,
  )

  private val mapAhs = {
    val mapCommonAh = new MapCommonAh(
      mmapRW = mmapsRW
    )
    val scMapDelayAh = new ScMapDelayAh(
      modelRW = mapDelayRW
    )
    foldHandlers( mapCommonAh, scMapDelayAh )
  }

  private val gridAh = new GridAh(
    api           = sc3UniApi,
    scRootRO      = rootRW,
    screenRO      = screenRO,
    modelRW       = gridRW
  )

  private val screenAh = new ScreenAh(
    modelRW = scScreenRW,
    rootRO  = rootRW
  )

  private val sTextAh = new STextAh(
    modelRW = searchTextRW
  )

  private val geoLocAh = new GeoLocAh(
    dispatcher  = this,
    modelRW     = scGeoLocRW
  )

  private val platformAh = new PlatformAh(
    modelRW = platformRW
  )


  private val beaconerAh = new BleBeaconerAh(
    modelRW     = beaconerRW,
    dispatcher  = this,
    onNearbyChange = Some { (nearby0, nearby2) =>
      val daemonS = daemonRW.value

      if (daemonS.state contains MDaemonStates.Work) {
        // Если что-то изменилось, то надо запустить обновление плитки.
        def finishWorkProcFx: Effect = {
          Effect.action( ScDaemonWorkProcess(isActive = false) )
        }

        val fx = if (nearby0 ===* nearby2) {
          // Ничего не изменилось: такое возможно при oneShot-режиме. Надо сразу деактивировать режим демонизации.
          finishWorkProcFx
        } else {
          // Что-то изменилось в списке маячков. Надо запустить обновление плитки.
          val shutOffAfterGrid = Effect.action {
            GridAfterUpdate(
              effect = finishWorkProcFx,
            )
          }
          _gridBleReloadFx + shutOffAfterGrid
        }
        Some( fx )

      } else {
        // Логика зависит от режима, который сейчас: работа демон или обычный режим вне демона.
        // Подписываемся на события изменения списка наблюдаемых маячков.
        OptionUtil.maybeOpt( nearby0 !===* nearby2 ) {
          // Если nodes-форма открыта, то надо отрендерить в ней инфу по маячкам.
          val nodesFormUpdateFxOpt = scNodesDiaAh.onBeaconsUpdatedFx( nearby2 )

          //println( "beacons changed: " + nearbyReportProxy.value.mkString("\n[", ",\n", "\n]") )
          val mroot = rootRW.value

          val gridUpdFxOpt = if (mroot.index.resp.isPending) {
            // Сигнал пришёл, когда уже идёт запрос плитки/индекса, то надо это уведомление закинуть в очередь.
            Option.when(
              !mroot.grid.afterUpdate.exists {
                case gla: GridLoadAds =>
                  gla.onlyMatching.exists { om =>
                    om.ntype contains[MNodeType] MNodeTypes.BleBeacon
                  }
                case _ => false
              }
            ) {
              // Нужно забросить в состояние плитки инфу о необходимости обновится после заливки исходной плитки.
              Effect.action {
                GridAfterUpdate( _gridBleReloadFx )
              }
            }

          } else {
            // Надо запустить пересборку плитки. Без Future, т.к. это - callback-функция.
            Some( _gridBleReloadFx )
          }

          // Объединить эффекты.
          (nodesFormUpdateFxOpt :: gridUpdFxOpt :: Nil)
            .iterator
            .flatten
            .mergeEffects
        }
      }
    }
  )

  private val wzFirstDiaAh = new WzFirstDiaAh(
    platformRO    = platformRW,
    screenInfoRO  = screenInfoRO,
    modelRW       = firstRunDiaRW,
    dispatcher    = this,
  )

  private val bootAh = new BootAh(
    modelRW = bootRW,
    circuit = this,
  )

  private val jdAh = new JdAh(
    modelRW = jdRuntimeRW,
  )

  private val scErrorDiaAh = new ScErrorDiaAh(
    modelRW = scErrorDiaRW,
    circuit = this,
  )

  private val dlAppAh = new DlAppAh(
    modelRW       = dlAppDiaRW,
    scAppApi      = scAppApi,
    indexStateRO  = inxStateRO,
  )

  private val scSettingsDiaAh = new ScSettingsDiaAh(
    modelRW = mkLensZoomRW( dialogsRW, MScDialogs.settings ),
  )


  private val notifyAh: Option[HandlerFunction] = {
    if (CordovaConstants.isCordovaPlatform()) {
      // Проверить готовность плагина нельзя: PlatformReady ещё не наступил.
      // Для cordova: контроллер нотификаций через cordova-plugin-local-notification:
      Some( new CordovaLocalNotificationAh(
        dispatcher  = this,
        modelRW     = mkLensZoomRW(osNotifyRW, MScOsNotifyS.cnl),
      ))
    } else if (Html5NotificationUtil.isApiAvailable()) {
      Some( new Html5NotificationApiAdp(
        dispatcher = this,
        modelRW = mkLensZoomRW( osNotifyRW, MScOsNotifyS.html5 )
      ))
    } else {
      // Тут было LOG.error(), но YandexBot постоянно сыпал этими ошибками на сервер. Поэтому тут просто логгирование для разраба.
      None
    }
  }


  /** Контроллер демона. */
  private val daemonBgModeAh: Option[HandlerFunction] = {
    if (CordovaConstants.isCordovaPlatform()) {
      Some(new CordovaBgModeAh(
        modelRW     = mkLensZoomRW( daemonRW, MScDaemon.cdvBgMode ),
        dispatcher  = this,
      ))
    } else {
      None
    }
  }

  private lazy val scDaemonAh = new ScDaemonAh(
    modelRW       = daemonRW,
    platfromRO    = platformRW,
    dispatcher    = this,
  )


  /** Выборочный контроллер sleep-таймера демона. */
  private val daemonSleepTimerAh: Option[HandlerFunction] = {
    Option.when( daemonBgModeAh.nonEmpty ) {
      if ( CordovaConstants.isCordovaPlatform() /*&& CordovaBgTimerAh.hasCordovaBgTimer()*/ ) {
        new CdvBgFetchAh(
          dispatcher = this,
          modelRW = mkLensZoomRW( daemonRW, MScDaemon.cdvBgFetch )
        )
        //new CordovaBgTimerAh(
        //  dispatcher = this,
        //  modelRW    = mkLensZoomRW( daemonRW, MScDaemon.cdvBgTimer ),
        //)
      } else {
        // TODO Не ясно, надо ли это активировать вообще? Может выкинуть (закомментить) этот контроллер? И его модель-состояние следом.
        new HtmlBgTimerAh(
          dispatcher = this,
          modelRW    = mkLensZoomRW( daemonRW, MScDaemon.htmlBgTimer ),
        )
      }
    }
  }

  /** Контроллер статуса интернет-подключения. */
  private val onLineAh = new OnLineAh(
    modelRW       = onLineRW,
    dispatcher    = this,
    retryActionRO = scErrorDiaRW.zoom( _.flatMap(_.retryAction) ),
    platformRO    = platformRW,
  )

  private val scConfAh = new ScConfAh(
    modelRW = confRW,
  )

  /** Контроллер управления отдельной формой логина. */
  private val scLoginDiaAh = new ScLoginDiaAh(
    modelRW = scLoginRW,
    getLoginFormCircuit = getLoginFormCircuit,
  )

  private val scNodesDiaAh = new ScNodesDiaAh(
    modelRW           = scNodesRW,
    getNodesCircuit   = getNodesFormCircuit,
    csrfRO            = csrfTokenRW,
    beaconsNearbyRO   = beaconsNearbyRO,
    isLoggedInRO      = loggedInRO,
  )

  private val csrfTokenAh = new CsrfTokenAh(
    modelRW       = csrfTokenRW,
    csrfTokenApi  = csrfTokenApi,
  )

  private val logOutAh = mkLogOutAh( logOutRW )

  private def advRcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl( routes )


  override protected val actionHandler: HandlerFunction = {
    // TODO На основе конкретного Action-интерфейса роутить сигнал сразу к нужному контроллеру.
    var acc = List.empty[HandlerFunction]

    // В самый хвост списка добавить дефолтовый обработчик для редких событий и событий, которые можно дропать.
    acc ::= tailAh
    acc ::= logOutAh
    acc ::= scNodesDiaAh
    acc ::= scLoginDiaAh
    acc ::= scConfAh

    if (CordovaConstants.isCordovaPlatform()) {
      acc ::= new LoginSessionAh(
        modelRW = loginSessionRW,
      )
    }

    acc ::= csrfTokenAh

    for (ah <- daemonBgModeAh) {
      acc ::= scDaemonAh
      acc ::= ah
    }

    for (ah <- daemonSleepTimerAh)
      acc ::= ah

    // Контроллер для нативного приложения.
    // TODO Не добавлять dlAppAh, когда не нужен (MPlatformS.isDlAppAvail()). Но доступ к MPlatformS отсюда невозможен из конструктора.
    acc ::= dlAppAh

    acc ::= scErrorDiaAh

    // Контроллер диалога настроек.
    acc ::= scSettingsDiaAh

    // Диалоги обычно закрыты. Тоже - в хвост.
    acc ::= wzFirstDiaAh

    // Контроллер загрузки
    acc ::= bootAh

    // Листенер инициализации роутера. Выкидывать его после окончания инициализации.
    //if ( !jsRouterRW.value.isReady ) {
      acc ::= new JsRouterInitAh(
        circuit = circuit,
        modelRW = jsRouterRW
      )
    //}

    // События уровня платформы.
    acc ::= platformAh
    acc ::= onLineAh

    // Контроллер нотификаций.
    for (ah <- notifyAh)
      acc ::= ah

    // События jd-шаблонов в плитке.
    acc ::= jdAh

    acc ::= sTextAh
    acc ::= geoTabAh    // TODO Объеденить с searchAh

    // Основные события индекса не частые, но доступны всегда *ДО*geoTabAh*:
    acc ::= indexAh

    //if ( indexWelcomeRW().nonEmpty )
      acc ::= new WelcomeAh( indexWelcomeRW )

    //if ( mapInitRW.value.ready )
      acc ::= mapAhs

    // Контроллеры СНАЧАЛА экрана, а ПОТОМ плитки. Нужно соблюдать порядок.
    acc ::= gridAh

    // Контроллер BLE-маячков. Сигналы приходят часто, поэтому его - ближе к голове списка.
    acc ::= beaconerAh

    // Геолокация довольно часто получает сообщения (когда активна), поэтому её -- тоже в начало списка контроллеров:
    acc ::= geoLocAh

    // Экран отрабатываем в начале, но необходимость этого под вопросом.
    acc ::= screenAh

    // Собрать все контроллеры в пачку.
    composeHandlers( acc: _* )
  }


  // Установить поддержку костыль-экшена DoNothing поверх основного actionHandler'а:
  // TODO Opt Или лучше в конец action handler? Оно жило в конце TailAh.
  addProcessor( DoNothingActionProcessor[MScRoot] )

  // Раскомментить, когда необходимо залогировать в консоль весь ход работы выдачи:
  //addProcessor( io.suggest.spa.LoggingAllActionsProcessor[MScRoot] )

  /** Когда наступает platform ready и BLE доступен,
    * надо попробовать активировать/выключить слушалку маячков BLE и разрешить геолокацию.
    */
  private def _dispatchBleBeaconerOnOff(): Boolean = {
    val plat = platformRW.value
    // TODO Не выполнять эффектов, если результата от них не будет (без фактической смены состояния или hardOff).
    val nextState = plat.isUsingNow
    (plat.hasBle && plat.isReady) && {
      Future {
        val msg = BtOnOff(
          isEnabled = nextState,
          opts = MBeaconerOpts(
            hardOff       = false,
            // Не долбить мозг юзеру системным запросом включения bluetooth.
            askEnableBt   = false,
            oneShot       = false,
          )
        )
        this.runEffectAction( msg )
      }
      nextState
    }
  }

  /** Реакция на готовность платформы к работе. */
  private def _onPlatformReady(): Unit = {
    // Активировать сборку Bluetooth-маячков:
    _dispatchBleBeaconerOnOff()

    // Принудительно пересчитать экран. В cordova данные экрана определяются через cordova-plugin-device.
    if (platformRW.value.isCordova)
      this.runEffectAction( ScreenResetNow )

    // Активировать поддержку нотификаций:
    if (notifyAh.nonEmpty) Future {
      val msg = NotifyStartStop(isStart = true)
      this.runEffectAction( msg )
    }

    // Инициализация демонизатора
    if ( daemonBgModeAh.nonEmpty && scDaemonAh.USE_BG_MODE ) Future {
      val daemonizerInitA = BgModeDaemonInit(
        initOpts = Some( MDaemonInitOpts(
          //events = MDaemonEvents(
          //  activated = ScDaemonWorkProcess,
          //),
          descr = MDaemonDescr(
            needBle = true,
          ),
          notification = Some( scNotifications.daemonNotifyOpts() ),
        ))
      )
      this.runEffectAction( daemonizerInitA )
    }

    this.runEffectAction( OnlineInit(true) )
  }


  // Отработать инициализацию js-роутера в самом начале конструктора.
  // По факту, инициализация уже наверное запущена в main(), но тут ещё и подписка на события...
  {
    // Сразу восстановить данные логина из БД:
    Future {
      if (platformRW.value.isCordova)
        this.runEffectAction( LoginSessionRestore )
    }

    // Немедленный запуск инициализации/загрузки
    Try {
      val needGeoLoc = routerState.canonicalRoute
        .fold(true)(_.needGeoLoc)
      val svcsTail = if (needGeoLoc) {
        MBootServiceIds.GeoLocDataAcc :: Nil
      } else {
        Nil
      }
      val bootMsg = Boot( MBootServiceIds.RcvrsMap :: svcsTail )
      this.runEffectAction( bootMsg )
    }

    // TODO Platform boot - унесено в BootAh.PlatformSvc
    val isPlatformReadyRO = mkLensZoomRO( platformRW, MPlatformS.isReady )
    // Начинаем юзать платформу прямо в конструкторе circuit. Это может быть небезопасно, поэтому тут try-catch для всей этой логики.
    try {
      // Лезть в состояние на стадии конструктора - плохая примета. Поэтому защищаемся от возможных косяков в будущем через try-обёртку вокруг zoom.value()
      if ( Try(isPlatformReadyRO.value) getOrElse false ) {
        // Платформа уже готова. Запустить эффект активации BLE-маячков.
        //LOG.log( "isPlatformReadyNowTry" )
        _onPlatformReady()
      } else {
        // Платформа не готова. Значит, надо бы дождаться готовности платформы и повторить попытку.
        //LOG.warn( WarnMsgs.PLATFORM_NOT_READY, msg = isPlatformReadyNowTry )

        // 2018-06-26: Добавить запасной таймер на случай если платформа так и не приготовится.
        val readyTimeoutId = DomQuick.setTimeout( 7000 ) { () =>
          if (!isPlatformReadyRO.value) {
            logger.error( ErrorMsgs.PLATFORM_READY_NOT_FIRED )
            // Без Future() т.к. это и так в контексте таймера.
            val msg = SetPlatformReady
            this.runEffectAction( msg )
          }
        }

        val sp = Promise[None.type]()
        val unSubscribePlatformReadyF = subscribe(isPlatformReadyRO) { isReadyNowProxy =>
          if (isReadyNowProxy.value) {
            DomQuick.clearTimeout( readyTimeoutId )
            // Запустить bluetooth-мониторинг.
            _onPlatformReady()
            // TODO Активировать фоновый GPS-мониторинг, чтобы видеть себя на карте. Нужен маркер на карте и спрашивался о переходе в новую локацию.
            sp.success(None)
          }
        }

        // Удалить подписку на platform-ready-события: она нужна только один раз: при запуске системы на слишком асинхронной платформе.
        sp.future
          .andThen { case _ => unSubscribePlatformReadyF() }
      }
    } catch { case ex: Throwable =>
      // Возникла ошибка от подготовки платформы прямо в конструкторе. Подавить, т.к. иначе всё встанет колом.
      logger.error( ErrorMsgs.CATCHED_CONSTRUCTOR_EXCEPTION, ex )
    }

    // Управление активированностью фоновой геолокации:
    def __dispatchGeoLocOnOff(enable: Boolean): Unit = {
      // Не диспатчить экшен, когда в этом нет необходимости. Проверять текущее состояние геолокации, прежде чем диспатчить экшен.
      val mroot = rootRW()
      val mgl = mroot.dev.geoLoc
      val isEnabled = mgl.switch.onOff contains[Boolean] true
      // Надо попытаться всё-равно включить геолокацию в DEV-mode, т.к. браузеры не дают геолокацию без ssl в локалке.
      val isToEnable = (
        enable && !isEnabled &&
        (Sc3ConfUtil.isDevMode || !mgl.switch.hardLock)
      )
      // Надо запускать обновление выдачи, если включение геолокации и панель карты закрыта.
      val isRunGeoLocInx = isToEnable && !mroot.index.search.panel.opened
      if (
        isToEnable || (!enable && isEnabled)
      ) {
        lazy val sctx = MScSwitchCtx(
          indexQsArgs = MScIndexArgs(
            geoIntoRcvr = true,
            retUserLoc  = false,
          ),
          demandLocTest = true,
        )
        Future {
          val msg = GeoLocOnOff(
            enabled  = enable,
            isHard   = false,
            scSwitch = OptionUtil.maybe(isRunGeoLocInx)(sctx)
          )
          this.runEffectAction( msg )
        }
        // При включении - запустить таймер геолокации, чтобы обновился index на новую геолокацию.
        if (isRunGeoLocInx) Future {
          // Передавать контекст, в котором явно указано, что это фоновая проверка смены локации, и всё должно быть тихо.
          val msg = GeoLocTimerStart(sctx)
          this.runEffectAction( msg )
        }
      }
    }


    // Реагировать на события активности приложения выдачи.
    subscribe( mkLensZoomRO(platformRW, MPlatformS.isUsingNow) ) { isUsingNowProxy =>
      // Отключать мониторинг BLE-маячков, когда платформа позволяет это делать.
      val isUsingNow = isUsingNowProxy.value
      val bleIsToEnable = _dispatchBleBeaconerOnOff()

      // Глушить фоновый GPS-мониторинг:
      __dispatchGeoLocOnOff(isUsingNow)

      // Если активация приложения, и есть отображаемые нотификации, то надо их затереть.
      if (isUsingNow && osNotifyRW.value.hasNotifications) {
        Future {
          val msg = CloseNotify(Nil)
          this.runEffectAction( msg )
        }
      }

      // Если уход в фон с активным мониторингом маячков, то надо уйти в бэкграунд.
      if (
        daemonSleepTimerAh.nonEmpty && (
          isUsingNow match {
            // включение: beaconer всегда выключен.
            case true  => bleIsToEnable
            // выключение
            case false => (beaconerRW.value.isEnabled contains[Boolean] true)
          }
        )
      ) {
        // Если сокрытие и включён bluetooth-мониторинг, то перейти в background-режим.
        this.runEffectAction( ScDaemonDozed(isActive = !isUsingNow) )
      }

      // В фоне не приходят события уведомления online/offline в cordova. TODO В браузере тоже надо пере-проверять?
      if (isUsingNow)
        this.runEffectAction( OnlineCheckConn )
    }

    Future {
      this.runEffectAction( LoadIndexRecents(clean = true) )
    }
  }


  /** Экшен для перезапроса с сервера только BLE-карточек плитки. */
  private def _gridBleReloadAction = GridLoadAds(
    clean         = true,
    ignorePending = true,
    silent        = OptionUtil.SomeBool.someTrue,
    onlyMatching  = Some( MScNodeMatchInfo(
      ntype = Some( MNodeTypes.BleBeacon ),
    )),
  )
  private def _gridBleReloadFx = Effect.action( _gridBleReloadAction )


  private def _errFrom(action: Any, ex: Throwable): Unit = {
    action match {
      case dAction: DAction =>
        val m = MScErrorDia(
          messageCode   = ErrorMsgs.SC_FSM_EVENT_FAILED,
          exceptionOpt  = Some( ex ),
          retryAction   = Some(dAction),
        )
        this.runEffectAction( SetErrorState(m) )
      case _ =>
        // should never happen
    }
  }

  override def handleFatal(action: Any, e: Throwable): Unit = {
    super.handleFatal(action, e)
    _errFrom(action, e)
  }

  override def handleError(msg: String): Unit = {
    super.handleError(msg)
    // TODO Это некритические ошибки. Надо будет потом приглушить, до snack'а например.
    val m = MScErrorDia(
      messageCode = msg,
    )
    this.runEffectAction( SetErrorState(m) )
  }

  override def handleEffectProcessingError[A](action: A, error: Throwable): Unit = {
    super.handleEffectProcessingError(action, error)
    _errFrom(action, error)
  }

}
