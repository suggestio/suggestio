package io.suggest.sc

import diode.{Effect, FastEq, ModelRO}
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.ble.beaconer.c.BleBeaconerAh
import io.suggest.ble.beaconer.m.{BtOnOff, MBeaconerS}
import io.suggest.common.empty.OptionUtil
import io.suggest.cordova.CordovaConstants
import io.suggest.dev.MScreen.MScreenFastEq
import io.suggest.dev.MScreenInfo.MScreenInfoFastEq
import io.suggest.dev.{JsScreenUtil, MScreenInfo}
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
import io.suggest.os.notify.api.cnl.CordovaLocalNotificationAdp
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.ads.MScNodeMatchInfo
import io.suggest.sc.c.dev.{GeoLocAh, PlatformAh, ScreenAh}
import io.suggest.sc.c._
import io.suggest.sc.c.boot.BootAh
import io.suggest.sc.c.dia.{ScErrorDiaAh, ScSettingsDiaAh, WzFirstDiaAh}
import io.suggest.sc.c.grid.{GridAh, GridFocusRespHandler, GridRespHandler}
import io.suggest.sc.c.inx.{ConfUpdateRah, IndexAh, IndexRah, WelcomeAh}
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
import io.suggest.sc.m.in.MScInternals
import io.suggest.sc.m.inx.{MScIndex, MScSwitchCtx}
import io.suggest.sc.m.menu.{MDlAppDia, MMenuS}
import io.suggest.sc.m.search.MGeoTabS.MGeoTabSFastEq
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.MSc3Conf
import io.suggest.sc.styl.{MScCssArgs, ScCss}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sc.u.api.IScAppApi
import io.suggest.sc.v.search.SearchCss
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.{DAction, DoNothingActionProcessor, FastEqUtil, OptFastEq}
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.CircuitUtil._
import org.scalajs.dom
import io.suggest.dev.MPlatformS
import io.suggest.os.notify.NotifyStartStop
import io.suggest.os.notify.api.html5.{Html5NotificationApiAdp, Html5NotificationUtil}

import scala.concurrent.{Future, Promise}
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
                  // Автоматические DI-аргументы:
                  sc3Api                    : ISc3Api,
                  scAppApi                  : IScAppApi,
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

  import io.suggest.ble.beaconer.m.MBeaconerS.MBeaconerSFastEq

  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  override protected def initialModel: MScRoot = {
    // Сначала надо подготовить конфиг, прочитав его со страницы (если он там есть).
    val scInit = Sc3ConfUtil.getFreshestInit()
      .getOrElse {
        val emsg = ErrorMsgs.GRID_CONFIGURATION_INVALID
        LOG.error( emsg )
        throw new NoSuchElementException( emsg )
      }

    val mscreen = JsScreenUtil.getScreen()
    val mplatform = PlatformAh.platformInit(this)

    val screenInfo = MScreenInfo(
      screen        = mscreen,
      unsafeOffsets = JsScreenUtil.getScreenUnsafeAreas(mscreen)
    )

    val scIndexResp = Pot.empty[MSc3IndexResp]

    MScRoot(
      dev = MScDev(
        screen = MScScreenS(
          info = screenInfo
        ),
        platform = mplatform
      ),
      index = MScIndex(
        resp = scIndexResp,
        search = MScSearch(
          geo = MGeoTabS(
            mapInit = MMapInitState(
              state = MMapS(scInit.mapProps)
            ),
            css = SearchCss( MSearchCssProps(
              screenInfo = screenInfo
            ))
          )
        ),
        scCss = ScCss(
          MScCssArgs.from(scIndexResp, screenInfo)
        ),
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
        conf = scInit.conf
      ),
    )
  }

  // Сразу подписаться на глобальные ошибки:
  {
    import io.suggest.sjs.common.vm.evtg.EventTargetVm._
    dom.window.addEventListener4s("error") { e: dom.ErrorEvent =>
      val msg = e.filename + " (" + e.lineno + "," + e.colno + ") " + e.message
      LOG.error(msg = msg)
      dispatch( SetErrorState(
        MScErrorDia(
          messageCode = MsgCodes.`Malfunction`,
          hint        = Some( msg ),
        )
      ))
    }
  }

  // Кэш zoom'ов модели:
  private[sc] val rootRW          = zoomRW(identity) { (_, new2) => new2 } ( MScRootFastEq )

  private[sc] val internalsRW     = mkLensRootZoomRW(this, MScRoot.internals)( MScInternalsFastEq )

  private[sc] val indexRW         = mkLensRootZoomRW(this, MScRoot.index)(MScIndexFastEq)
  private[sc] val titleOptRO      = indexRW.zoom( _.resp.toOption.flatMap(_.name) )( OptFastEq.Plain )
  private val indexWelcomeRW      = mkLensZoomRW(indexRW, MScIndex.welcome)( OptFastEq.Wrapped(MWelcomeStateFastEq) )

  val scCssRO: ModelRO[ScCss]     = mkLensZoomRO( indexRW, MScIndex.scCss )( FastEq.AnyRefEq )

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

  private val devRW               = mkLensRootZoomRW(this, MScRoot.dev)( MScDevFastEq )
  private val scScreenRW          = mkLensZoomRW(devRW, MScDev.screen)( MScScreenSFastEq )
  private val scGeoLocRW          = mkLensZoomRW(devRW, MScDev.geoLoc)( MScGeoLocFastEq )

  private val confRO              = mkLensZoomRO(internalsRW, MScInternals.conf)( MSc3Conf.MSc3ConfFastEq )
  private val rcvrsMapUrlRO       = mkLensZoomRO(confRO, MSc3Conf.rcvrsMapUrl)( FastEq.AnyRefEq )

  private[sc] val platformRW      = mkLensZoomRW(devRW, MScDev.platform)( MPlatformS.MPlatformSFastEq )

  private val beaconerRW          = mkLensZoomRW(devRW, MScDev.beaconer)( MBeaconerSFastEq )

  private val dialogsRW           = mkLensRootZoomRW(this, MScRoot.dialogs )( MScDialogsFastEq )
  private[sc] val firstRunDiaRW   = mkLensZoomRW(dialogsRW, MScDialogs.first)( MWzFirstOuterSFastEq )

  private val bootRW              = mkLensZoomRW(internalsRW, MScInternals.boot)( MScBootFastEq )
  private[sc] val jsRouterRW      = mkLensZoomRW(internalsRW, MScInternals.jsRouter )( FastEqUtil.AnyRefFastEq )
  private val scErrorDiaRW        = mkLensZoomRW(dialogsRW, MScDialogs.error)( OptFastEq.Wrapped(MScErrorDia.MScErrorDiaFastEq) )

  private val menuRW              = mkLensZoomRW( indexRW, MScIndex.menu )( MMenuS.MMenuSFastEq )
  private val dlAppDiaRW          = mkLensZoomRW( menuRW, MMenuS.dlApp )( MDlAppDia.MDlAppDiaFeq )

  private val inxStateRO          = mkLensZoomRO( indexRW, MScIndex.state )

  private val screenInfoRO        = mkLensZoomRO(scScreenRW, MScScreenS.info)( MScreenInfoFastEq )
  private val screenRO            = mkLensZoomRO(screenInfoRO, MScreenInfo.screen)( MScreenFastEq )

  private val osNotifyRW          = mkLensZoomRW(devRW, MScDev.osNotify)( MScOsNotifyS.MScOsNotifyFeq )


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
        }
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
    api             = sc3Api,
    scRootRO        = rootRW,
    rcvrsMapApi     = advRcvrsMapApi,
    screenInfoRO    = screenInfoRO,
    rcvrMapArgsRO   = rcvrsMapUrlRO,
  )

  private val indexAh = new IndexAh(
    api     = sc3Api,
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
    api           = sc3Api,
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
    dispatcher  = this
  )

  private val wzFirstDiaAh = new WzFirstDiaAh(
    platformRO  = platformRW,
    modelRW     = firstRunDiaRW,
    dispatcher  = this,
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

  private val menuNativeAppAh = new DlAppAh(
    modelRW       = dlAppDiaRW,
    scAppApi      = scAppApi,
    indexStateRO  = inxStateRO,
  )

  private val scSettingsDiaAh = new ScSettingsDiaAh(
    modelRW = mkLensZoomRW( dialogsRW, MScDialogs.settings ),
  )

  private val notifyAhOrNull: HandlerFunction = {
    if (CordovaConstants.isCordovaPlatform()) {
      // Для cordova: контроллер нотификаций через cordova-plugin-local-notification:
      new CordovaLocalNotificationAdp(
        dispatcher  = this,
        modelRW     = mkLensZoomRW(osNotifyRW, MScOsNotifyS.cnl),
      )
    } else if (Html5NotificationUtil.isApiAvailable()) {
      new Html5NotificationApiAdp(
        dispatcher = this,
        modelRW = mkLensZoomRW( osNotifyRW, MScOsNotifyS.html5 )
      )
    } else {
      null
    }
  }
  private def _hasNotifyAh: Boolean = notifyAhOrNull != null


  private def advRcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl( ScJsRoutes )


  override protected val actionHandler: HandlerFunction = {
    // TODO На основе конкретного Action-интерфейса роутить сигнал сразу к нужному контроллеру.
    var acc = List.empty[HandlerFunction]

    // TODO Opt Здесь много вызовов model.value. Может быть эффективнее будет один раз прочитать всю модель, и сверять её разные поля по мере необходимости?

    // В самый хвост списка добавить дефолтовый обработчик для редких событий и событий, которые можно дропать.
    acc ::= tailAh

    // Контроллер для нативного приложения.
    acc ::= menuNativeAppAh

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

    // Контроллер нотификаций.
    if ( _hasNotifyAh )
      acc ::= notifyAhOrNull

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
  //addProcessor( LoggingAllActionsProcessor[MScRoot] )

  /** Когда наступает platform ready и BLE доступен,
    * надо попробовать активировать/выключить слушалку маячков BLE и разрешить геолокацию.
    */
  private def _dispatchBleBeaconerOnOff(): Unit = {
    try {
      val plat = platformRW.value
      if (plat.hasBle && plat.isReady) {
        //LOG.warn( "ok, dispatching ble on/off", msg = plat )
        Future {
          val msg = BtOnOff( isEnabled = plat.isUsingNow )
          dispatch( msg )
        }
      }
    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.CORDOVA_BLE_REQUIRE_FAILED, ex )
    }
  }

  /** Реакция на готовность платформы к работе. */
  private def _onPlatformReady(): Unit = {
    // Активировать сборку Bluetooth-маячков:
    _dispatchBleBeaconerOnOff()
    // Активировать поддержку нотификаций:
    if (_hasNotifyAh)
      Future( dispatch( NotifyStartStop(isStart = true) ) )
  }


  // Отработать инициализацию js-роутера в самом начале конструктора.
  // По факту, инициализация уже наверное запущена в main(), но тут ещё и подписка на события...
  {
    // Немедленный запуск инициализации/загрузки
    Try {
      val needGeoLoc = routerState.canonicalRoute
        .fold(true)(_.needGeoLoc)
      val svcsTail = if (needGeoLoc) {
        MBootServiceIds.GeoLocDataAcc :: Nil
      } else {
        Nil
      }
      dispatch(
        Boot( MBootServiceIds.RcvrsMap :: svcsTail )
      )
    }

    // TODO Platform boot - унесено в BootAh.PlatformSvc
    val isPlatformReadyRO = mkLensZoomRO( platformRW, MPlatformS.isReady )
    // Начинаем юзать платформу прямо в конструкторе circuit. Это может быть небезопасно, поэтому тут try-catch для всей этой логики.
    try {
      // Лезть в состояние на стадии конструктора - плохая примета. Поэтому защищаемся от возможных косяков в будущем через try-обёртку вокруг zoom.value()
      if ( Try(isPlatformReadyRO.value).getOrElse(false) ) {
        // Платформа уже готова. Запустить эффект активации BLE-маячков.
        //LOG.log( "isPlatformReadyNowTry" )
        _onPlatformReady()
      } else {
        // Платформа не готова. Значит, надо бы дождаться готовности платформы и повторить попытку.
        //LOG.warn( WarnMsgs.PLATFORM_NOT_READY, msg = isPlatformReadyNowTry )

        // 2018-06-26: Добавить запасной таймер на случай если платформа так и не приготовится.
        val readyTimeoutId = DomQuick.setTimeout( 7000 ) { () =>
          if (!isPlatformReadyRO.value) {
            LOG.error( ErrorMsgs.PLATFORM_READY_NOT_FIRED )
            // Без Future() т.к. это и так в контексте таймера.
            dispatch( SetPlatformReady )
          }
        }

        val sp = Promise[None.type]()
        val cancelF = subscribe(isPlatformReadyRO) { isReadyNowProxy =>
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
          .andThen { case _ => cancelF() }
      }
    } catch { case ex: Throwable =>
      // Возникла ошибка от подготовки платформы прямо в конструкторе. Подавить, т.к. иначе всё встанет колом.
      LOG.error( ErrorMsgs.CATCHED_CONSTRUCTOR_EXCEPTION, ex )
    }

    // Управление активированностью фоновой геолокации:
    def __dispatchGeoLocOnOff(enable: Boolean): Unit = {
      // Не диспатчить экшен, когда в этом нет необходимости. Проверять текущее состояние геолокации, прежде чем диспатчить экшен.
      val mroot = rootRW()
      val mgl = mroot.dev.geoLoc
      val isEnabled = mgl.switch.onOff contains true
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
          dispatch( msg )
        }
        // При включении - запустить таймер геолокации, чтобы обновился index на новую геолокацию.
        if (isRunGeoLocInx) Future {
          // Передавать контекст, в котором явно указано, что это фоновая проверка смены локации, и всё должно быть тихо.
          dispatch( GeoLocTimerStart(sctx) )
        }
      }
    }

    // Реагировать на события активности приложения выдачи.
    subscribe( mkLensZoomRO(platformRW, MPlatformS.isUsingNow) ) { isUsingNowProxy =>
      // Отключать мониторинг BLE-маячков, когда платформа позволяет это делать.
      val isUsingNow = isUsingNowProxy.value
      _dispatchBleBeaconerOnOff()

      // Глушить фоновый GPS-мониторинг:
      __dispatchGeoLocOnOff(isUsingNow)
    }

    // Подписаться на события изменения списка наблюдаемых маячков.
    // Не подписываться без необходимости? Но для этого надо подписываться на platform.isBleAvail, т.е. смысла в оптимизации нет.
    subscribe( mkLensZoomRO(beaconerRW, MBeaconerS.nearbyReport) ) { _ =>
      //println( "beacons changed: " + nearbyReportProxy.value.mkString("\n[", ",\n", "\n]") )
      val mroot = rootRW.value

      if (mroot.index.resp.isPending) {
        // Сигнал пришёл, когда уже идёт запрос плитки/индекса, то надо это уведомление закинуть в очередь.
        if (
          !mroot.grid.afterUpdate.exists {
            case gla: GridLoadAds =>
              gla.onlyMatching.exists { om =>
                om.ntype contains[MNodeType] MNodeTypes.BleBeacon
              }
            case _ => false
          }
        ) {
          // Нужно забросить в состояние плитки инфу о необходимости обновится после заливки исходной плитки.
          dispatch( GridAfterUpdate( Effect.action(_gridBleReloadAction) ) )
        }

      } else {
        // Надо запустить пересборку плитки. Без Future, т.к. это - callback-функция.
        val glaAction = _gridBleReloadAction
        dispatch( glaAction )
        //val glaFx = _gridBleReloadAction.toEffectPure
        //this.runEffect( glaFx, glaAction )
      }
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


  private def _errFrom(action: Any, ex: Throwable): Unit = {
    action match {
      case dAction: DAction =>
        val m = MScErrorDia(
          messageCode   = ErrorMsgs.SC_FSM_EVENT_FAILED,
          exceptionOpt  = Some( ex ),
          retryAction   = Some(dAction),
        )
        dispatch( SetErrorState(m) )
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
    dispatch( SetErrorState(m) )
  }

  override def handleEffectProcessingError[A](action: A, error: Throwable): Unit = {
    super.handleEffectProcessingError(action, error)
    _errFrom(action, error)
  }

}
