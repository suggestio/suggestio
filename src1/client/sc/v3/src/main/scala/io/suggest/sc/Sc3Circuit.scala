package io.suggest.sc

import diode.{Effect, FastEq, ModelRW}
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.radio.beacon.{BeaconerAh, IBeaconsListenerApi, IBeaconerAction, MBeaconerS}
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.cordova.background.fetch.CdvBgFetchAh
import io.suggest.cordova.background.mode.CordovaBgModeAh
import io.suggest.daemon.{IDaemonAction, IDaemonSleepAction, MDaemonState, MDaemonStates}
import io.suggest.dev.MScreen.MScreenFastEq
import io.suggest.dev.MScreenInfo.MScreenInfoFastEq
import io.suggest.dev.{JsScreenUtil, MPlatformS, MScreenInfo}
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdConf
import io.suggest.jd.render.c.JdAh
import io.suggest.jd.render.u.JdUtil
import io.suggest.maps.c.{MapCommonAh, RcvrMarkersInitAh}
import io.suggest.maps.m.{IMapsAction, IRcvrMarkersInitAction, MMapS}
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
import io.suggest.sc.index.{MSc3IndexResp, MScIndexes}
import io.suggest.sc.m._
import io.suggest.sc.m.boot.MScBoot.MScBootFastEq
import io.suggest.sc.m.boot.{Boot, IBootAction, MBootServiceIds, MSpaRouterState}
import io.suggest.sc.m.dev.{MScDev, MScOsNotifyS, MScScreenS}
import io.suggest.sc.m.dia.{MScDialogs, MScLoginS}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.grid.{GridAfterUpdate, GridLoadAds, MGridCoreS, MGridS, MScAdData}
import io.suggest.sc.m.in.{MInternalInfo, MScDaemon, MScInternals}
import io.suggest.sc.m.inx.{IIndexAction, IWelcomeAction, MScIndex, MScIndexState}
import io.suggest.sc.m.menu.{IScAppAction, MDlAppDia, MMenuS}
import io.suggest.sc.m.search.MGeoTabS.MGeoTabSFastEq
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.{MSc3Conf, MSc3Init}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sc.u.api.{IScAppApi, IScStuffApi, IScUniApi}
import io.suggest.sc.v.search.SearchCss
import io.suggest.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2._
import io.suggest.spa.{CircuitUtil, DAction, DoNothing, DoNothingActionProcessor, FastEqUtil, IHwBtnAction, OptFastEq}
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.CircuitUtil._
import org.scalajs.dom
import io.suggest.event.DomEvents
import io.suggest.geo.GeoLocApi
import io.suggest.id.login.LoginFormCircuit
import io.suggest.id.login.c.session.{LogOutAh, SessionAh}
import io.suggest.id.login.m.ILogoutAction
import io.suggest.id.login.m.session.MLogOutDia
import io.suggest.jd.render.m.{IGridAction, IJdAction}
import io.suggest.lk.c.{CsrfTokenAh, ICsrfTokenApi}
import io.suggest.lk.m.{ICsrfTokenAction, ISessionAction}
import io.suggest.lk.nodes.form.LkNodesFormCircuit
import io.suggest.lk.r.plat.PlatformCssStatic
import io.suggest.os.notify.IOsNotifyAction
import io.suggest.os.notify.api.html5.{Html5NotificationApiAdp, Html5NotificationUtil}
import io.suggest.react.r.ComponentCatch
import io.suggest.sc.c.android.{IIntentAction, ScIntentsAh}
import io.suggest.sc.c.in.{BootAh, ScDaemonAh}
import io.suggest.sc.m.dia.first.IWz1Action
import io.suggest.sc.m.inx.save.MIndexesRecentOuter
import io.suggest.sc.m.styl.MScCssArgs
import io.suggest.sc.v.styl.ScCss
import io.suggest.sc.v.toast.ScNotifications
import io.suggest.spa.delay.{ActionDelayerAh, IDelayAction}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{JSON, JavaScriptException}
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
                  sc3UniApi                 : => IScUniApi,
                  scAppApi                  : => IScAppApi,
                  scStuffApi                : => IScStuffApi,
                  csrfTokenApi              : => ICsrfTokenApi,
                  geoLocApis                : () => LazyList[GeoLocApi],
                  beaconApis                : () => LazyList[IBeaconsListenerApi],
                  //nfcApiOpt                 : => Option[INfcApi],
                  mkLogOutAh                : ModelRW[MScRoot, Option[MLogOutDia]] => LogOutAh[MScRoot],
                )
  extends CircuitLog[MScRoot]
  with ReactConnector[MScRoot]
{ circuit =>

  import MScIndex.MScIndexFastEq
  import m.in.MScInternals.MScInternalsFastEq
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
    val mplatform = PlatformAh.platformInit()

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

    def searchCssEmpty(isBar: Boolean) = SearchCss( MSearchCssProps(
      screenInfo = screenInfo,
      searchBar = isBar,
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
            css = searchCssEmpty(true),
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
            jdRuntime = JdUtil.prepareJdRuntime(jdConf).make,
          )
        )
      },
      internals = MScInternals(
        conf = conf2,
        info = MInternalInfo(
          indexesRecents = MIndexesRecentOuter(
            searchCss = searchCssEmpty(false),
            saved = Pot.empty[MScIndexes],
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
  private[sc] def internalsInfoRW = mkLensZoomRW( internalsRW, MScInternals.info )
  private[sc] def csrfTokenRW     = mkLensZoomRW( internalsInfoRW, MInternalInfo.csrfToken )
  private[sc] def currRouteRW     = mkLensZoomRW( internalsInfoRW, MInternalInfo.currRoute )

  private[sc] val indexRW         = mkLensRootZoomRW(this, MScRoot.index)(MScIndexFastEq)
  private[sc] def titlePartsRO    = rootRW.zoom [List[String]] { mroot =>
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
      scAdData  <- mroot.grid.core.ads.interactAdOpt
      adData    <- scAdData.getLabel.data.toOption
      if adData.isOpened
      focAdTitle <- adData.title
    } {
      acc ::= focAdTitle
    }

    acc
  }( FastEq.ValueEq )

  private def indexWelcomeRW      = mkLensZoomRW(indexRW, MScIndex.welcome)( OptFastEq.Wrapped(MWelcomeStateFastEq) )
  private[sc] def scCssRO         = mkLensZoomRO(indexRW, MScIndex.scCss)

  private val searchRW            = mkLensZoomRW(indexRW, MScIndex.search)( MScSearchFastEq )
  private val geoTabRW            = mkLensZoomRW(searchRW, MScSearch.geo)( MGeoTabSFastEq )

  private val mapInitRW           = mkLensZoomRW(geoTabRW, MGeoTabS.mapInit)( MMapInitStateFastEq )
  private val mmapsRW             = mkLensZoomRW(mapInitRW, MMapInitState.state)( MMapSFastEq4Map )
  private def searchTextRW        = mkLensZoomRW(searchRW, MScSearch.text)( MScSearchTextFastEq )
  private[sc] val geoTabDataRW    = mkLensZoomRW(geoTabRW, MGeoTabS.data)( MGeoTabData.MGeoTabDataFastEq )
  private val mapDelayRW          = mkLensZoomRW(geoTabDataRW, MGeoTabData.delay)( OptFastEq.Wrapped(MMapDelay.MMapDelayFastEq) )

  private def rcvrRespRW          = {
    geoTabRW.zoomRW( _.data.rcvrsCache.map(_.resp) ) {
      (geoTab0, potResp2) =>
        val rcvrsCache2 = for (gnr <- potResp2) yield {
          MSearchRespInfo(
            textQuery   = None,
            resp        = gnr,
          )
        }

        var modF = MGeoTabS.data
          .composeLens( MGeoTabData.rcvrsCache )
          .set( rcvrsCache2 )

        // И сразу залить в основное состояние карты ресиверов, если там нет иных данных.
        val geoTab_mapInit_rcvrs_LENS = MGeoTabS.mapInit
          .composeLens( MMapInitState.rcvrs )
        val currRcvrs0 = geoTab_mapInit_rcvrs_LENS.get(geoTab0)
        if (currRcvrs0.isEmpty || (currRcvrs0 ===* geoTab0.data.rcvrsCache))
          modF = modF andThen geoTab_mapInit_rcvrs_LENS.set( rcvrsCache2 )

        modF( geoTab0 )
    }
  }

  private def gridRW              = mkLensRootZoomRW(this, MScRoot.grid)
  private def gridCoreRW          = mkLensZoomRW( gridRW, MGridS.core )
  private def jdRuntimeRW         = mkLensZoomRW( gridCoreRW, MGridCoreS.jdRuntime )( FastEqUtil.AnyRefFastEq )

  private[sc] val devRW           = mkLensRootZoomRW(this, MScRoot.dev)( MScDevFastEq )
  private val scScreenRW          = mkLensZoomRW(devRW, MScDev.screen)( MScScreenSFastEq )
  private val scGeoLocRW          = mkLensZoomRW(devRW, MScDev.geoLoc)( MScGeoLocFastEq )
  private def onLineRW            = mkLensZoomRW(devRW, MScDev.onLine)

  private def confRW              = mkLensZoomRW(internalsRW, MScInternals.conf)( MSc3Conf.MSc3ConfFastEq )
  private def rcvrsMapUrlRO       = mkLensZoomRO(confRW, MSc3Conf.rcvrsMapUrl)( FastEq.AnyRefEq )

  private[sc] val platformRW      = mkLensZoomRW(devRW, MScDev.platform)( MPlatformS.MPlatformSFastEq )
  private[sc] def platformCssRO   = mkLensZoomRO(devRW, MScDev.platformCss)

  private[sc] val beaconerRW      = mkLensZoomRW(devRW, MScDev.beaconer)( MBeaconerSFastEq )
  private[sc] def beaconerEnabled = beaconerRW.zoom(_.isEnabled contains true)
  private[sc] def beaconsRO       = mkLensZoomRO( beaconerRW, MBeaconerS.beacons )
  private[sc] val hasBleRO        = mkLensZoomRO( beaconerRW, MBeaconerS.hasBle ).zoom( _ contains[Boolean] true )

  private val dialogsRW           = mkLensRootZoomRW(this, MScRoot.dialogs )( MScDialogsFastEq )
  private[sc] def firstRunDiaRW   = mkLensZoomRW(dialogsRW, MScDialogs.first)( MWzFirstOuterSFastEq )
  private[sc] def scLoginRW       = mkLensZoomRW(dialogsRW, MScDialogs.login)
  private val scNodesRW           = mkLensZoomRW(dialogsRW, MScDialogs.nodes)

  private def bootRW              = mkLensZoomRW(internalsRW, MScInternals.boot)( MScBootFastEq )
  private[sc] def jsRouterRW      = mkLensZoomRW(internalsRW, MScInternals.jsRouter )( FastEqUtil.AnyRefFastEq )
  private def scErrorDiaRW        = mkLensZoomRW(dialogsRW, MScDialogs.error)( OptFastEq.Wrapped(MScErrorDia.MScErrorDiaFastEq) )

  private def menuRW              = mkLensZoomRW( indexRW, MScIndex.menu )( MMenuS.MMenuSFastEq )
  private def dlAppDiaRW          = mkLensZoomRW( menuRW, MMenuS.dlApp )( MDlAppDia.MDlAppDiaFeq )

  private[sc] def inxStateRO      = mkLensZoomRO( indexRW, MScIndex.state )

  private val screenInfoRO        = mkLensZoomRO(scScreenRW, MScScreenS.info)( MScreenInfoFastEq )
  private def screenRO            = mkLensZoomRO(screenInfoRO, MScreenInfo.screen)( MScreenFastEq )

  private def osNotifyRW          = mkLensZoomRW(devRW, MScDev.osNotify)( MScOsNotifyS.MScOsNotifyFeq )
  def loggedInRO                  = indexRW.zoom(_.isLoggedIn)

  private def daemonRW            = mkLensZoomRW( internalsRW, MScInternals.daemon )
  private[sc] def loginSessionRW  = mkLensZoomRW( scLoginRW, MScLoginS.session )
  private def logOutRW            = mkLensZoomRW( scLoginRW, MScLoginS.logout )

  private def delayerRW           = mkLensZoomRW( internalsRW, MScInternals.delayer )

  /** Текущая открытая карточка, пригодная для операций над ней: размещение в маячке, например. */
  private[sc] def focusedAdRO     = gridRW.zoom [Option[MScAdData]] { mgrid =>
    for {
      scAdLoc <- mgrid.core.ads.interactAdOpt
      scAd = scAdLoc.getLabel
      if scAd.canEdit
    } yield scAd
  }

  // notifications
  private def scNotifications = new ScNotifications(
    rootRO = rootRW,
  )

  // Action-Handler'ы

  /** Хвостовой контроллер -- сюда свалено разное управление выдачей. */
  private def tailAh = {
    // Списки обработчиков ответов ScUniApi с сервера и resp-action в этих ответах.
    // Часть модулей является универсальной, поэтому шарим хвост списка между обоими списками:
    val mixed: LazyList[IRespWithActionHandler] = (
      new GridRespHandler(
        isDoOsNotify = rootRW.zoom { mroot =>
          // Разрешается делать нотификацию уровня ОС только если:
          // 1. Есть разрешение на нотификации.
          (mroot.dev.osNotify.hasPermission contains true) //&&
          // TODO 2. Приложение скрыто, и требует привлечения внимания, и запрос был в фоне.
          //!mroot.dev.platform.isUsingNow
        },
        scNotifications = scNotifications,
      ) #::
        new GridFocusRespHandler #::
        new IndexRah #::
        new NodesSearchRah( screenInfoRO ) #::
        LazyList.empty
      )

    val respHandlers: LazyList[IRespHandler] = mixed

    val respActionHandlers: LazyList[IRespActionHandler] =
      new ConfUpdateRah #::
      mixed

    new TailAh(
      modelRW               = rootRW,
      routerCtl             = routerState.routerCtl,
      scRespHandlers        = respHandlers,
      scRespActionHandlers  = respActionHandlers,
      scStuffApi            = scStuffApi,
    )
  }


  private def geoTabAh = new GeoTabAh(
    modelRW         = geoTabRW,
    api             = sc3UniApi,
    scRootRO        = rootRW,
    screenInfoRO    = screenInfoRO,
  )

  private def rcvrMarkersInitAh = new RcvrMarkersInitAh(
    modelRW         = rcvrRespRW,
    api             = advRcvrsMapApi,
    argsRO          = rcvrsMapUrlRO,
    isOnlineRoOpt   = Some( onLineRW.zoom(_.isOnline) ),
  )

  private def indexAh = new IndexAh(
    api     = sc3UniApi,
    modelRW = indexRW,
    rootRO  = rootRW,
  )

  private val mapAhs: HandlerFunction = {
    val mapCommonAh = new MapCommonAh(
      mmapRW = mmapsRW
    )
    val scMapDelayAh = new ScMapDelayAh(
      modelRW = mapDelayRW
    )
    foldHandlers( mapCommonAh, scMapDelayAh )
  }

  // val - это из-за повторяющегося GridScroll-эффекта
  private val gridAh: HandlerFunction = new GridAh(
    api           = sc3UniApi,
    scRootRO      = rootRW,
    screenRO      = screenRO,
    modelRW       = gridRW
  )

  private def screenAh = new ScreenAh(
    modelRW = scScreenRW,
    rootRO  = rootRW
  )

  private def searchTextAh = new STextAh(
    modelRW = searchTextRW
  )

  private val geoLocAh: HandlerFunction = new GeoLocAh(
    dispatcher   = this,
    modelRW      = scGeoLocRW,
    geoLocApis   = geoLocApis,
  )

  private def platformAh = new PlatformAh(
    modelRW           = platformRW,
    rootRO            = rootRW,
    dispatcher        = this,
    scNotifications   = scNotifications,
  )

  private def delayerAh = new ActionDelayerAh(
    modelRW = delayerRW,
  )

  private val beaconerAh: HandlerFunction = new BeaconerAh(
    modelRW     = beaconerRW,
    dispatcher  = this,
    bcnsIsSilentRO = scNodesRW.zoom(!_.opened),
    osFamilyOpt = CircuitUtil.mkLensZoomRO( platformRW, MPlatformS.osFamily ).value,
    beaconApis  = beaconApis,
    onNearbyChange = Some { (nearby0, nearby2) =>
      var fxAcc = List.empty[Effect]

      // Отправить эффект изменения в списке маячков
      if (scNodesRW.value.opened) {
        fxAcc ::= Effect.action {
          scNodesDiaAh.handleBeaconsDetected()
          DoNothing
        }
      }

      val canUpdateBleGrid = inxStateRO.value.isBleGridAds

      /** Экшен для перезапроса с сервера только BLE-карточек плитки. */
      def _gridBleReloadFx: Effect = {
        Effect.action {
          GridLoadAds(
            clean         = true,
            ignorePending = true,
            silent        = OptionUtil.SomeBool.someTrue,
            onlyMatching  = Some( MScNodeMatchInfo(
              ntype = Some( MNodeTypes.BleBeacon ),
            )),
          )
        }
      }

      if (daemonRW.value.state contains[MDaemonState] MDaemonStates.Work) {
        // Если что-то изменилось, то надо запустить обновление плитки.
        def finishWorkProcFx: Effect = {
          Effect.action( ScDaemonWorkProcess(isActive = false) )
        }

        if ( !canUpdateBleGrid || (nearby0 ===* nearby2) ) {
          // Ничего не изменилось: такое возможно при oneShot-режиме. Надо сразу деактивировать режим демонизации.
          fxAcc ::= finishWorkProcFx
        } else {
          // Что-то изменилось в списке маячков. Надо запустить обновление плитки.
          fxAcc ::= Effect.action {
            GridAfterUpdate(
              effect = finishWorkProcFx,
            )
          }
          fxAcc ::= _gridBleReloadFx
        }
        fxAcc.mergeEffects

      } else {
        // Логика зависит от режима, который сейчас: работа демон или обычный режим вне демона.
        // Подписываемся на события изменения списка наблюдаемых маячков.
        OptionUtil.maybeOpt( nearby0 !===* nearby2 ) {
          val mroot = rootRW.value

          val gridUpdFxOpt = if (mroot.index.resp.isPending) {
            // Сигнал пришёл, когда уже идёт запрос плитки/индекса, то надо это уведомление закинуть в очередь.
            Option.when(
              !mroot.grid.afterUpdate.exists {
                case gla: GridLoadAds =>
                  gla.onlyMatching.exists { om =>
                    om.ntype.exists { ntype =>
                      MNodeTypes.lkNodesUserCanCreate contains[MNodeType] ntype
                    }
                  }
                case _ => false
              }
            ) {
              // Нужно забросить в состояние плитки инфу о необходимости обновится после заливки исходной плитки.
              (Effect.action( GridAfterUpdate( _gridBleReloadFx )) :: fxAcc)
                .mergeEffects
                .get
            }

          } else if (!canUpdateBleGrid) {
            // Выставлен запрет на изменение плитки.
            None

          } else {
            // Надо запустить пересборку плитки. Без Future, т.к. это - callback-функция.
            (_gridBleReloadFx :: fxAcc).mergeEffects
          }

          gridUpdFxOpt
        }
      }
    },
  )

  private def wzFirstDiaAh = new WzFirstDiaAh(
    platformRO    = platformRW,
    screenInfoRO  = screenInfoRO,
    hasBleRO      = hasBleRO,
    modelRW       = firstRunDiaRW,
    dispatcher    = this,
    //nfcApi        = nfcApiOpt,
  )

  private def bootAh = new BootAh(
    modelRW = bootRW,
    circuit = this,
  )

  private def jdAh = new JdAh(
    modelRW = jdRuntimeRW,
  )

  private def scErrorDiaAh = new ScErrorDiaAh(
    modelRW = scErrorDiaRW,
    circuit = this,
  )

  private def dlAppAh = new DlAppAh(
    modelRW       = dlAppDiaRW,
    scAppApi      = scAppApi,
    indexStateRO  = inxStateRO,
  )

  private def scSettingsDiaAh = new ScSettingsDiaAh(
    modelRW = mkLensZoomRW( dialogsRW, MScDialogs.settings ),
  )


  private def notifyAh: Option[HandlerFunction] = {
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
  private def daemonBgModeAh: HandlerFunction = {
    new CordovaBgModeAh(
      modelRW     = mkLensZoomRW( daemonRW, MScDaemon.cdvBgMode ),
      dispatcher  = this,
    )
  }

  private def scDaemonAh = new ScDaemonAh(
    modelRW       = daemonRW,
    platfromRO    = platformRW,
    dispatcher    = this,
  )


  /** Выборочный контроллер sleep-таймера демона. */
  private def daemonSleepTimerAh: HandlerFunction = {
      new CdvBgFetchAh(
        dispatcher = this,
        modelRW = mkLensZoomRW( daemonRW, MScDaemon.cdvBgFetch )
      )
    //if ( CordovaConstants.isCordovaPlatform() /*&& CordovaBgTimerAh.hasCordovaBgTimer()*/ ) {
      //new CordovaBgTimerAh(
      //  dispatcher = this,
      //  modelRW    = mkLensZoomRW( daemonRW, MScDaemon.cdvBgTimer ),
      //)
    /*} else {
        // TODO Не ясно, надо ли это активировать вообще? Может выкинуть (закомментить) этот контроллер? И его модель-состояние следом.
        new HtmlBgTimerAh(
          dispatcher = this,
          modelRW    = mkLensZoomRW( daemonRW, MScDaemon.htmlBgTimer ),
        )
      }*/
  }

  /** Контроллер статуса интернет-подключения. */
  private def onLineAh = new OnLineAh(
    modelRW       = onLineRW,
    dispatcher    = this,
    retryActionRO = scErrorDiaRW.zoom( _.flatMap(_.retryAction) ),
    platformRO    = platformRW,
  )

  private def scConfAh = new ScConfAh(
    modelRW  = confRW,
    scInitRO = rootRW.zoom(_.toScInit),
  )

  /** Контроллер управления отдельной формой логина. */
  private def scLoginDiaAh = new ScLoginDiaAh(
    modelRW = scLoginRW,
    getLoginFormCircuit = getLoginFormCircuit,
  )

  private def scNodesDiaAh = new ScNodesDiaAh(
    modelRW           = scNodesRW,
    getNodesCircuit   = getNodesFormCircuit,
    sc3Circuit        = this,
  )

  private def csrfTokenAh = new CsrfTokenAh(
    modelRW       = csrfTokenRW,
    csrfTokenApi  = csrfTokenApi,
    onError       = Some { () =>
      OnlineCheckConn.toEffectPure
    },
  )

  private def logOutAh = mkLogOutAh( logOutRW )
  private def welcomeAh = new WelcomeAh( indexWelcomeRW )
  private def sessionAh = new SessionAh(
    modelRW = loginSessionRW,
  )
  private def jsRouterInitAh = new JsRouterInitAh(
    modelRW = jsRouterRW
  )

  private def scIntentsAh = new ScIntentsAh(
    modelRW = rootRW,
  )

  //private def nfcAh = new NfcAh(
  //)

  private def advRcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl( routes )


  /** Функция-роутер экшенов в конкретные контроллеры. */
  override protected val actionHandler: HandlerFunction = { (mroot, action) =>
    val ctlOrNull: HandlerFunction = action match {
      case _: IBeaconerAction           => beaconerAh
      case _: IGridAction               => gridAh
      case _: IMapsAction               => mapAhs
      case _: IGeoLocAction             => geoLocAh
      case _: IJdAction                 => jdAh
      case _: IScTailAction             => tailAh
      case _: IOnlineAction             => onLineAh
      case _: IIndexAction              => indexAh
      case _: ISearchTextAction         => searchTextAh
      case _: IScScreenAction           => screenAh
      case _: IPlatformAction           => platformAh
      case _: IScNodesAction            => scNodesDiaAh
      case _: IWelcomeAction            => welcomeAh
      case _: ISessionAction            => sessionAh
      case _: IGeoTabAction             => geoTabAh
      case _: ICsrfTokenAction          => csrfTokenAh
      case _: IOsNotifyAction           => notifyAh.orNull
      case _: IScErrorAction            => scErrorDiaAh
      case _: IScAppAction              => dlAppAh
      case _: IScSettingsAction         => scSettingsDiaAh
      case _: IScDaemonAction           => scDaemonAh
      case _: IIntentAction             => scIntentsAh
      case _: IDaemonAction             => daemonBgModeAh
      case _: IDaemonSleepAction        => daemonSleepTimerAh
      case _: IHwBtnAction              => tailAh
      // редкие варианты:
      case _: IScConfAction             => scConfAh
      case _: IScLoginAction            => scLoginDiaAh
      case _: ILogoutAction             => logOutAh
      case _: IBootAction               => bootAh
      case _: IWz1Action                => wzFirstDiaAh
      case _: IRcvrMarkersInitAction    => rcvrMarkersInitAh
      case _: IScJsRouterInitAction     => jsRouterInitAh
      case _: IDelayAction              => delayerAh
      case _: ComponentCatch            => tailAh
    }
    Option( ctlOrNull )
      .flatMap( _(mroot, action) )
  }


  // Установить поддержку костыль-экшена DoNothing поверх основного actionHandler'а:
  // TODO Opt Или лучше в конец action handler? Оно жило в конце TailAh.
  addProcessor( DoNothingActionProcessor[MScRoot] )

  // Раскомментить, когда необходимо залогировать в консоль весь ход работы выдачи:
  //addProcessor( io.suggest.spa.LoggingAllActionsProcessor[MScRoot] )


  {
    // Сразу запустить инициализация платформы, которая запустит инициализацию остального:
    Future {
      this.runEffectAction( PlatformReady() )
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
      val rcvrMapBi = MBootServiceIds.RcvrsMap
      val bootMsg = Boot( rcvrMapBi :: svcsTail )
      this.runEffectAction( bootMsg )
    }
  }


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
    val error2 = error match {
      case jsErr: JavaScriptException =>
        Try {
          var msgAcc = JSON.stringify( jsErr.exception.asInstanceOf[js.Any] )

          for ( msg <- Option(jsErr.getMessage()) ) {
            val msg2 = msg + " " + msgAcc
            msgAcc = msg2
          }

          new RuntimeException( msgAcc, error )
        }
          .getOrElse( error )

      case _ => error
    }
    super.handleEffectProcessingError(action, error2)
    _errFrom(action, error2)
  }

}
