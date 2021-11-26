package io.suggest.sc

import diode.FastEq
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.radio.beacon.{IBeaconerAction, IBeaconsListenerApi, MBeaconerS}
import io.suggest.cordova.CordovaConstants
import io.suggest.cordova.background.mode.CordovaBgModeAh
import io.suggest.daemon.{IDaemonAction, IDaemonSleepAction}
import io.suggest.dev.MScreen.MScreenFastEq
import io.suggest.dev.MScreenInfo.MScreenInfoFastEq
import io.suggest.dev.{MPlatformS, MScScreenS, MScreen, MScreenInfo}
import io.suggest.jd.MJdConf
import io.suggest.jd.render.c.JdAh
import io.suggest.jd.render.u.JdUtil
import io.suggest.maps.{IMapsAction, IRcvrMarkersInitAction, MMapProps, MMapS}
import io.suggest.maps.MMapS.MMapSFastEq4Map
import io.suggest.msg._
import io.suggest.os.notify.api.cnl.CordovaLocalNotificationAh
import io.suggest.sc.controller.dev.{GeoLocAh, PlatformAh, ScGeoTimerAh, ScScreenAh}
import io.suggest.sc.controller._
import io.suggest.sc.controller.dia.{ScErrorDiaAh, ScSettingsDiaAh, WzFirstDiaAh}
import io.suggest.sc.controller.grid.{GridAh, GridFocusRespHandler, GridRespHandler, LocationButtonAh}
import io.suggest.sc.controller.inx.{ConfUpdateRah, IndexAh, IndexRah, NodesRecentAh, ScConfAh, WelcomeAh}
import io.suggest.sc.controller.jsrr.JsRouterInitAh
import io.suggest.sc.controller.menu.DlAppAh
import io.suggest.sc.controller.search._
import io.suggest.sc.index.MScIndexes
import io.suggest.sc.model._
import io.suggest.sc.model.boot.MScBoot.MScBootFastEq
import io.suggest.sc.model.boot.{Boot, IBootAction, MBootServiceIds, MSpaRouterState}
import io.suggest.sc.model.dev.{MScDev, MScOsNotifyS}
import io.suggest.sc.model.dia.{MScDialogs, MScLoginS}
import io.suggest.sc.model.dia.err.MScErrorDia
import io.suggest.sc.model.grid.{MGridCoreS, MGridS, MScAdData}
import io.suggest.sc.model.in.{MInternalInfo, MScDaemon, MScInternals}
import io.suggest.sc.model.inx.{IIndexAction, IWelcomeAction, MScIndex, MScIndexState}
import io.suggest.sc.model.menu.{IScAppAction, MDlAppDia, MMenuS}
import io.suggest.sc.model.search.MGeoTabS.MGeoTabSFastEq
import io.suggest.sc.model.search._
import io.suggest.sc.sc3.{IScRespAction, MSc3Conf, MSc3Init}
import io.suggest.sc.util.ScGeoUtil
import io.suggest.sc.util.api.{IScAppApi, IScStuffApi, IScUniApi}
import io.suggest.sc.view.search.SearchCss
import io.suggest.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.{DAction, DoNothingActionProcessor, FastEqUtil, IHwBtnAction, OptFastEq}
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.CircuitUtil._
import io.suggest.geo.{GeoLocApi, MGeoPoint}
import io.suggest.grid.ScGridScrollUtil
import io.suggest.id.login.c.session.SessionAh
import io.suggest.id.login.m.ILogoutAction
import io.suggest.jd.render.m.{IGridAction, IJdAction}
import io.suggest.leaflet.{ILeafletGeoLocAction, ILeafletGeoLocAh}
import io.suggest.lk.api.ILkLangApi
import io.suggest.lk.c.{CsrfTokenAh, ICsrfTokenApi}
import io.suggest.lk.m.{ICsrfTokenAction, ISessionAction}
import io.suggest.lk.r.plat.PlatformCssStatic
import io.suggest.os.notify.IOsNotifyAction
import io.suggest.os.notify.api.html5.{Html5NotificationApiAdp, Html5NotificationUtil}
import io.suggest.react.r.ComponentCatch
import io.suggest.sc.controller.android.{IIntentAction, ScIntentsAh}
import io.suggest.sc.controller.showcase.{ScErrorAh, ScRespAh}
import io.suggest.sc.controller.in.{BootAh, ScDaemonAh, ScLangAh}
import io.suggest.sc.model.dia.first.IWz1Action
import io.suggest.sc.model.inx.save.MIndexesRecentOuter
import io.suggest.sc.model.styl.MScCssArgs
import io.suggest.sc.view.styl.ScCss
import io.suggest.sc.view.toast.ScNotifications
import io.suggest.scroll.IScrollApi
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
abstract class ScCommonCircuit
  extends CircuitLog[MScRoot]
  with ReactConnector[MScRoot]
{ circuit =>

  def sc3UniApi: IScUniApi
  def scAppApi: IScAppApi
  def scStuffApi: IScStuffApi
  def lkLangApi: ILkLangApi
  def csrfTokenApi: ICsrfTokenApi
  def geoLocApis(): LazyList[GeoLocApi]
  def beaconApis(): LazyList[IBeaconsListenerApi]
  /** Leaflet monkey-patched internal location API calls controller. */
  def leafletGeoLocAhOpt: Option[ILeafletGeoLocAh[MScRoot]]
  def scGeoUtil: ScGeoUtil
  def isNeedBootPerms(): Boolean
  def isNeedGeoLocOnResume(): Boolean
  def scrollApiOpt: Option[IScrollApi]
  def scGridScrollUtil: ScGridScrollUtil

  import MScIndex.MScIndexFastEq
  import model.in.MScInternals.MScInternalsFastEq
  import MScDev.MScDevFastEq
  import MScScreenS.MScScreenSFastEq
  import model.dev.MScGeoLoc.MScGeoLocFastEq
  import model.inx.MWelcomeState.MWelcomeStateFastEq

  import MScSearch.MScSearchFastEq
  import model.search.MScSearchText.MScSearchTextFastEq
  import MScRoot.MScRootFastEq
  import MMapInitState.MMapInitStateFastEq
  import io.suggest.sc.model.dia.MScDialogs.MScDialogsFastEq
  import io.suggest.sc.model.dia.first.MWzFirstOuterS.MWzFirstOuterSFastEq

  import MBeaconerS.MBeaconerSFastEq

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  /** Abstracted code of initialModel() method. */
  protected trait InitialModel {

    def scInitDefault = MSc3Init(
      mapProps = MMapProps(
        center = MGeoPoint.Examples.RU_SPB_CENTER,
        zoom   = MMapProps.ZOOM_DEFAULT,
      ),
      conf = MSc3Conf()
    )

    def scInit: MSc3Init

    def scConf: MSc3Conf
    def generation: Long
    def _mscreen: MScreen

    def mplatform: MPlatformS

    def scRoot: MScRoot = {
      val mscreen = _mscreen
      val screenInfo = MScreenInfo(
        screen        = mscreen,
        //unsafeOffsets = HwScreenUtil.etScreenUnsafeAreas( mscreen ),
      )

      def searchCssEmpty(isBar: Boolean) = SearchCss( MSearchCssProps(
        screenInfo = screenInfo,
        searchBar = isBar,
      ))
      val _mplatform = mplatform

      MScRoot(
        dev = MScDev(
          screen = MScScreenS(
            info = screenInfo,
          ),
          platform = _mplatform,
          platformCss   = PlatformCssStatic(
            isRenderIos = _mplatform.isUseIosStyles,
          ),
        ),
        index = MScIndex(
          resp = Pot.empty,
          search = MScSearch(
            geo = MGeoTabS(
              mapInit = MMapInitState(
                state = MMapS(scInit.mapProps)
              ),
              css = searchCssEmpty(true),
            ),
          ),
          scCss = ScCss(
            MScCssArgs.from( None, screenInfo )
          ),
          state = MScIndexState(
            generation = generation,
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
          conf = scConf,
          info = MInternalInfo(
            indexesRecents = MIndexesRecentOuter(
              searchCss = searchCssEmpty(false),
              saved = Pot.empty[MScIndexes],
            ),
          ),
        ),
      )
    }

  }


  // Кэш zoom'ов модели:
  private[sc] val rootRW          = zoomRW(identity) { (_, new2) => new2 } ( MScRootFastEq )

  private[sc] val internalsRW     = mkLensRootZoomRW(this, MScRoot.internals)( MScInternalsFastEq )
  private[sc] def internalsInfoRW = mkLensZoomRW( internalsRW, MScInternals.info )
  private[sc] def csrfTokenRW     = mkLensZoomRW( internalsInfoRW, MInternalInfo.csrfToken )
  private[sc] def currRouteRW     = mkLensZoomRW( internalsInfoRW, MInternalInfo.currRoute )

  private[sc] val indexRW         = mkLensRootZoomRW(this, MScRoot.index)(MScIndexFastEq)
  private[sc] def titlePartsRO    = zoom [List[String]] { mroot =>
    var acc = List.empty[String]

    // Заголовок узла.
    for {
      inxResp <- mroot.index.resp
      nodeName <- inxResp.resp.name
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

  def indexWelcomeRW              = mkLensZoomRW(indexRW, MScIndex.welcome)( OptFastEq.Wrapped(MWelcomeStateFastEq) )
  private[sc] def scCssRO         = mkLensZoomRO(indexRW, MScIndex.scCss)

  private val searchRW            = mkLensZoomRW(indexRW, MScIndex.search)( MScSearchFastEq )
  private val geoTabRW            = mkLensZoomRW(searchRW, MScSearch.geo)( MGeoTabSFastEq )

  private val mapInitRW           = mkLensZoomRW(geoTabRW, MGeoTabS.mapInit)( MMapInitStateFastEq )
  def mmapsRW                     = mkLensZoomRW(mapInitRW, MMapInitState.state)( MMapSFastEq4Map )
  private def searchTextRW        = mkLensZoomRW(searchRW, MScSearch.text)( MScSearchTextFastEq )
  private[sc] val geoTabDataRW    = mkLensZoomRW(geoTabRW, MGeoTabS.data)( MGeoTabData.MGeoTabDataFastEq )
  def mapDelayRW                  = mkLensZoomRW(geoTabDataRW, MGeoTabData.delay)( OptFastEq.Wrapped(MMapDelay.MMapDelayFastEq) )

  def rcvrRespRW = {
    geoTabRW.zoomRW( _.data.rcvrsCache.map(_.resp) ) {
      (geoTab0, potResp2) =>
        val rcvrsCache2 = for (gnr <- potResp2) yield {
          MSearchRespInfo(
            textQuery   = None,
            resp        = gnr,
          )
        }

        var modF = MGeoTabS.data
          .andThen( MGeoTabData.rcvrsCache )
          .replace( rcvrsCache2 )

        // И сразу залить в основное состояние карты ресиверов, если там нет иных данных.
        val geoTab_mapInit_rcvrs_LENS = MGeoTabS.mapInit
          .andThen( MMapInitState.rcvrs )
        val currRcvrs0 = geoTab_mapInit_rcvrs_LENS.get(geoTab0)
        if (currRcvrs0.isEmpty || (currRcvrs0 ===* geoTab0.data.rcvrsCache))
          modF = modF andThen (geoTab_mapInit_rcvrs_LENS replace rcvrsCache2)

        modF( geoTab0 )
    }
  }

  private def gridRW              = mkLensRootZoomRW(this, MScRoot.grid)
  private def gridCoreRW          = mkLensZoomRW( gridRW, MGridS.core )
  private def jdRuntimeRW         = mkLensZoomRW( gridCoreRW, MGridCoreS.jdRuntime )( FastEqUtil.AnyRefFastEq )

  private[sc] val devRW           = mkLensRootZoomRW(this, MScRoot.dev)( MScDevFastEq )
  private val scScreenRW          = mkLensZoomRW(devRW, MScDev.screen)( MScScreenSFastEq )
  private[sc] val scGeoLocRW      = mkLensZoomRW(devRW, MScDev.geoLoc)( MScGeoLocFastEq )
  def onLineRW                    = mkLensZoomRW(devRW, MScDev.onLine)

  private def confRW              = mkLensZoomRW(internalsRW, MScInternals.conf)( MSc3Conf.MSc3ConfFastEq )
  def rcvrsMapUrlRO               = mkLensZoomRO(confRW, MSc3Conf.rcvrsMapUrl)( FastEq.AnyRefEq )

  private[sc] val platformRW      = mkLensZoomRW(devRW, MScDev.platform)( MPlatformS.MPlatformSFastEq )
  private[sc] def platformCssRO   = mkLensZoomRO(devRW, MScDev.platformCss)

  private[sc] val beaconerRW      = mkLensZoomRW(devRW, MScDev.beaconer)( MBeaconerSFastEq )
  private[sc] def beaconerEnabled = beaconerRW.zoom(_.isEnabled contains true)
  private[sc] def beaconsRO       = mkLensZoomRO( beaconerRW, MBeaconerS.beacons )

  private val dialogsRW           = mkLensRootZoomRW(this, MScRoot.dialogs )( MScDialogsFastEq )
  private[sc] def wzFirstOuterRW   = mkLensZoomRW(dialogsRW, MScDialogs.first)( MWzFirstOuterSFastEq )
  private[sc] def scLoginRW       = mkLensZoomRW(dialogsRW, MScDialogs.login)
  val scNodesRW                   = mkLensZoomRW(dialogsRW, MScDialogs.nodes)

  private def bootRW              = mkLensZoomRW(internalsRW, MScInternals.boot)( MScBootFastEq )
  private[sc] def jsRouterRW      = mkLensZoomRW(internalsRW, MScInternals.jsRouter )( FastEqUtil.AnyRefFastEq )
  def scErrorDiaRW                = mkLensZoomRW(dialogsRW, MScDialogs.error)( OptFastEq.Wrapped(MScErrorDia.MScErrorDiaFastEq) )

  private def menuRW              = mkLensZoomRW( indexRW, MScIndex.menu )( MMenuS.MMenuSFastEq )
  private def dlAppDiaRW          = mkLensZoomRW( menuRW, MMenuS.dlApp )( MDlAppDia.MDlAppDiaFeq )

  private[sc] def inxStateRO      = mkLensZoomRO( indexRW, MScIndex.state )

  private val screenInfoRO        = mkLensZoomRO(scScreenRW, MScScreenS.info)( MScreenInfoFastEq )
  private def screenRO            = mkLensZoomRO(screenInfoRO, MScreenInfo.screen)( MScreenFastEq )

  private def osNotifyRW          = mkLensZoomRW(devRW, MScDev.osNotify)( MScOsNotifyS.MScOsNotifyFeq )
  def loggedInRO                  = indexRW.zoom(_.isLoggedIn)

  def daemonRW                    = mkLensZoomRW( internalsRW, MScInternals.daemon )
  private[sc] def loginSessionRW  = mkLensZoomRW( scLoginRW, MScLoginS.session )
  private[sc] def logOutRW        = mkLensZoomRW( scLoginRW, MScLoginS.logout )

  private def delayerRW           = mkLensZoomRW( internalsRW, MScInternals.delayer )

  private[sc] def reactCtxRW      = mkLensZoomRW( internalsInfoRW, MInternalInfo.reactCtx )
  private[sc] def languageOrSystemRO = reactCtxRW.zoom( _.languageOrSystem )

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

  private def scRespAh: HandlerFunction = {
    // Allow notifications, if notification permission granted.
    lazy val osScNotificationsOpt = Option.when( osNotifyRW.value.hasPermission contains[Boolean] true )( scNotifications )

    // Списки обработчиков ответов ScUniApi с сервера и resp-action в этих ответах.
    // Часть модулей является универсальной, поэтому шарим хвост списка между обоими списками:
    val mixed: LazyList[IRespWithActionHandler] = (
      new GridRespHandler(
        scNotificationsOpt  = osScNotificationsOpt,
        scrollApiOpt        = scrollApiOpt,
        scGridScrollUtil    = scGridScrollUtil,
      ) #::
        new GridFocusRespHandler( scGridScrollUtil ) #::
        new IndexRah #::
        new NodesSearchRah( screenInfoRO ) #::
        LazyList.empty
      )

    val respHandlers: LazyList[IRespHandler] = mixed

    val respActionHandlers: LazyList[IRespActionHandler] =
      new ConfUpdateRah #::
      mixed

    new ScRespAh(
      modelRW               = rootRW,
      scRespHandlers        = respHandlers,
      scRespActionHandlers  = respActionHandlers,
    )
  }

  def scRoutingAh: HandlerFunction

  private def indexesRecentAh: HandlerFunction = new NodesRecentAh(
    modelRW               = mkLensZoomRW( internalsInfoRW, MInternalInfo.inxRecents ),
    scStuffApi            = scStuffApi,
    scRootRO              = rootRW,
  )

  def scHwButtonsAh: HandlerFunction

  private def geoTabAh: HandlerFunction = new GeoTabAh(
    modelRW         = geoTabRW,
    api             = sc3UniApi,
    scRootRO        = rootRW,
    screenInfoRO    = screenInfoRO,
    scGeoUtil       = scGeoUtil,
  )

  def rcvrMarkersInitAh: HandlerFunction

  private def indexAh: HandlerFunction = new IndexAh(
    api     = sc3UniApi,
    modelRW = indexRW,
    rootRO  = rootRW,
    scGeoUtil = scGeoUtil,
  )

  /** Map-related stuff should be dropped in showcase-ssr. */
  def mapAhs: HandlerFunction

  // val - это из-за повторяющегося GridScroll-эффекта
  private val gridAh: HandlerFunction = new GridAh(
    api           = sc3UniApi,
    scRootRO      = rootRW,
    screenRO      = screenRO,
    modelRW       = gridRW,
    scGridScrollUtil = scGridScrollUtil,
  )

  private def scScreenAh: HandlerFunction = new ScScreenAh(
    modelRW = scScreenRW,
    rootRO  = rootRW
  )

  private def searchTextAh: HandlerFunction = new STextAh(
    modelRW = searchTextRW
  )

  private val geoLocAh: HandlerFunction = new GeoLocAh(
    dispatcher   = this,
    modelRW      = scGeoLocRW,
    geoLocApis   = geoLocApis,
    leafletGeoLocAhOpt = leafletGeoLocAhOpt,
  )

  private def platformAh: HandlerFunction = new PlatformAh(
    modelRW           = platformRW,
    rootRO            = rootRW,
    dispatcher        = this,
    scNotifications   = scNotifications,
    needGeoLocRO      = isNeedGeoLocOnResume,
  )

  private def delayerAh: HandlerFunction = new ActionDelayerAh(
    modelRW = delayerRW,
  )

  /** Beaconer is moved Sc3Module. Just because we need minimal showcase-ssr with minimal deps. */
  def beaconerAh: HandlerFunction


  private def wzFirstDiaAh: HandlerFunction = new WzFirstDiaAh(
    platformRO    = platformRW,
    modelRW       = wzFirstOuterRW,
    sc3Circuit    = this,
  )

  private def bootAh: HandlerFunction = new BootAh(
    modelRW = bootRW,
    circuit = this,
    needBootPermsRO = isNeedBootPerms,
  )

  private def jdAh: HandlerFunction = new JdAh(
    modelRW = jdRuntimeRW,
  )

  private def scErrorDiaAh: HandlerFunction = new ScErrorDiaAh(
    modelRW = scErrorDiaRW,
    circuit = this,
  )

  private def dlAppAh: HandlerFunction = new DlAppAh(
    modelRW       = dlAppDiaRW,
    scAppApi      = scAppApi,
    indexStateRO  = inxStateRO,
  )

  private def scSettingsDiaAh: HandlerFunction = new ScSettingsDiaAh(
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

  private def scDaemonAh: HandlerFunction = new ScDaemonAh(
    modelRW       = daemonRW,
    platfromRO    = platformRW,
    dispatcher    = this,
  )


  /** Выборочный контроллер sleep-таймера демона. */
  def daemonSleepTimerAh: HandlerFunction

  /** Контроллер статуса интернет-подключения. */
  def onLineAh: HandlerFunction

  private def scConfAh: HandlerFunction = new ScConfAh(
    modelRW  = confRW,
    scInitRO = rootRW.zoom(_.toScInit),
  )

  /** Контроллер управления отдельной формой логина. */
  def scLoginDiaAh: HandlerFunction

  def scNodesDiaAh: HandlerFunction

  private def csrfTokenAh: HandlerFunction = new CsrfTokenAh(
    modelRW       = csrfTokenRW,
    csrfTokenApi  = csrfTokenApi,
    onError       = Some( OnlineCheckConn.maybeFxOpt ),
  )

  def logOutAh: HandlerFunction
  def welcomeAh: HandlerFunction
  private def sessionAh: HandlerFunction = new SessionAh(
    modelRW = loginSessionRW,
  )
  private def jsRouterInitAh: HandlerFunction = new JsRouterInitAh(
    modelRW = jsRouterRW
  )

  private def scIntentsAh: HandlerFunction = new ScIntentsAh(
    modelRW = rootRW,
  )

  private def scLangAh: HandlerFunction = new ScLangAh(
    modelRW    = reactCtxRW,
    scStuffApi = scStuffApi,
    isLoggedIn = loggedInRO,
    lkLangApi  = lkLangApi,
    platformRO = platformRW,
  )

  private def locationButtonAh: HandlerFunction = new LocationButtonAh(
    modelRW = rootRW,  // rootRW - stub-placeholder for value by now, root model not really needed.
  )

  private def scGeoTimerAh: HandlerFunction = new ScGeoTimerAh(
    modelRW = rootRW,
  )

  private def scErrorAh: HandlerFunction = new ScErrorAh(
    modelRW = rootRW,
  )




  /** Функция-роутер экшенов в конкретные контроллеры. */
  override protected val actionHandler: HandlerFunction = { (mroot, action) =>
    val handler: HandlerFunction = action match {
      case _: IBeaconerAction           => beaconerAh
      case _: IGridAction               => gridAh
      case _: IMapsAction               => mapAhs
      case _: IGeoLocAction             => geoLocAh
      case _: IJdAction                 => jdAh
      case _: IScRoutingAction          => scRoutingAh
      case _: IOnlineAction             => onLineAh
      case _: IIndexAction              => indexAh
      case _: IScRespAction             => scRespAh
      case _: ISearchTextAction         => searchTextAh
      case _: IScScreenAction           => scScreenAh
      case _: IPlatformAction           => platformAh
      case _: IScNodesAction            => scNodesDiaAh
      case _: IWelcomeAction            => welcomeAh
      case _: ISessionAction            => sessionAh
      case _: IGeoTabAction             => geoTabAh
      case _: ICsrfTokenAction          => csrfTokenAh
      case _: IGeoLocTimerAction        => scGeoTimerAh
      case _: ILocationButtonAction     => locationButtonAh
      case _: IScIndexesRecentAction    => indexesRecentAh
      case _: IOsNotifyAction           => notifyAh.orNull
      case _: IScErrorAction            => scErrorDiaAh
      case _: IScAppAction              => dlAppAh
      case _: IScSettingsAction         => scSettingsDiaAh
      case _: IScDaemonAction           => scDaemonAh
      case _: IIntentAction             => scIntentsAh
      case _: IDaemonAction             => daemonBgModeAh
      case _: IDaemonSleepAction        => daemonSleepTimerAh
      case _: IHwBtnAction              => scHwButtonsAh
      // редкие варианты:
      case _: IScConfAction             => scConfAh
      case _: IScLoginAction            => scLoginDiaAh
      case _: ILogoutAction             => logOutAh
      case _: IBootAction               => bootAh
      case _: IWz1Action                => wzFirstDiaAh
      case _: IScLangAction             => scLangAh
      case _: IRcvrMarkersInitAction    => rcvrMarkersInitAh
      case _: IScJsRouterInitAction     => jsRouterInitAh
      case _: IDelayAction              => delayerAh
      case _: ComponentCatch            => scErrorAh
      case _: ILeafletGeoLocAction      => leafletGeoLocAhOpt.get
    }
    handler( mroot, action )
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
      val bootMsg = Boot(
        MBootServiceIds.PermissionsGui ::
        MBootServiceIds.RcvrsMap ::
        Nil
      )

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
