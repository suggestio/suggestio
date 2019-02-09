package io.suggest.sc

import diode.{FastEq, ModelRO}
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.ble.beaconer.c.BleBeaconerAh
import io.suggest.ble.beaconer.m.BtOnOff
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.MScreen.MScreenFastEq
import io.suggest.dev.MScreenInfo.MScreenInfoFastEq
import io.suggest.dev.{JsScreenUtil, MPxRatios, MScreenInfo}
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MLocEnv
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.JdCss
import io.suggest.maps.c.MapCommonAh
import io.suggest.maps.m.MMapS
import io.suggest.maps.m.MMapS.MMapSFastEq4Map
import io.suggest.maps.u.AdvRcvrsMapApiHttpViaUrl
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs, WarnMsgs}
import io.suggest.routes.ScJsRoutes
import io.suggest.sc.ads.MAdsSearchReq
import io.suggest.sc.c.dev.{GeoLocAh, PlatformAh, ScreenAh}
import io.suggest.sc.c._
import io.suggest.sc.c.boot.BootAh
import io.suggest.sc.c.dia.FirstRunDialogAh
import io.suggest.sc.c.grid.{GridAh, GridFocusRespHandler, GridRespHandler}
import io.suggest.sc.c.inx.{ConfUpdateRah, IndexAh, IndexRah, WelcomeAh}
import io.suggest.sc.c.jsrr.JsRouterInitAh
import io.suggest.sc.c.search._
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs}
import io.suggest.sc.m._
import io.suggest.sc.m.boot.MScBoot.MScBootFastEq
import io.suggest.sc.m.boot.{Boot, MBootServiceIds}
import io.suggest.sc.m.dev.{MScDev, MScScreenS}
import io.suggest.sc.m.dia.MScDialogs
import io.suggest.sc.m.grid.{GridLoadAds, MGridCoreS, MGridS}
import io.suggest.sc.m.inx.{MScIndex, MScSwitchCtx}
import io.suggest.sc.m.jsrr.MJsRouterS.MJsRouterSFastEq
import io.suggest.sc.m.search.MGeoTabS.MGeoTabSFastEq
import io.suggest.sc.m.search._
import io.suggest.sc.sc3.{MSc3Conf, MScCommonQs, MScQs, Sc3Pages}
import io.suggest.sc.styl.{MScCssArgs, ScCss}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sc.v.search.SearchCss
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick
import io.suggest.spa.{DoNothingActionProcessor, OptFastEq}
import io.suggest.spa.CircuitUtil._
import japgolly.scalajs.react.extra.router.RouterCtl

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
                  routerCtl                 : RouterCtl[Sc3Pages],
                  // Автоматические DI-аргументы:
                  api                       : ISc3Api,
                )
  extends CircuitLog[MScRoot]
  with ReactConnector[MScRoot]
{ circuit =>

  import MScIndex.MScIndexFastEq
  import m.MScInternals.MScInternalsFastEq
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

  import MEsUuId.Implicits._
  import io.suggest.dev.MPlatformS.MPlatformSFastEq
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

    /*
    val screenInfo = {
      // TODO задетектить unsafeOffsets
      val some20 = Some(20)
      MScreenInfo(
        screen = mscreen,
        unsafeOffsets = io.suggest.dev.MTlbr(
          topO = some20,
          leftO = some20
        )
      )
    }
    */

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
        )
      ),
      grid = {
        val (gridColsCount, gridSzMult) = GridAh.fullGridConf(mscreen)
        val jdConf = MJdConf(
          isEdit            = false,
          gridColumnsCount  = gridColsCount,
          szMult            = gridSzMult
        )
        MGridS(
          core = MGridCoreS(
            jdConf = jdConf,
            jdCss  = JdCss( MJdCssArgs(conf = jdConf) )
          )
        )
      },
      internals = MScInternals(
        conf = scInit.conf
      )
    )
  }

  // Кэш zoom'ов модели:
  private[sc] val rootRW          = zoomRW(identity) { (_, new2) => new2 } ( MScRootFastEq )

  private[sc] val internalsRW     = mkLensRootZoomRW(this, MScRoot.internals)( MScInternalsFastEq )
  private[sc] val jsRouterRW      = mkLensZoomRW( internalsRW, MScInternals.jsRouter )( MJsRouterSFastEq )

  private[sc] val indexRW         = mkLensRootZoomRW(this, MScRoot.index)(MScIndexFastEq)
  private[sc] val titleOptRO      = indexRW.zoom( _.resp.toOption.flatMap(_.name) )( OptFastEq.Plain )
  private[sc] val indexWelcomeRW  = mkLensZoomRW(indexRW, MScIndex.welcome)( OptFastEq.Wrapped(MWelcomeStateFastEq) )

  val scCssRO: ModelRO[ScCss]     = mkLensZoomRO( indexRW, MScIndex.scCss )( FastEq.AnyRefEq )

  private[sc] val searchRW        = mkLensZoomRW(indexRW, MScIndex.search)( MScSearchFastEq )
  private[sc] val geoTabRW        = mkLensZoomRW(searchRW, MScSearch.geo)( MGeoTabSFastEq )

  private[sc] val mapInitRW       = mkLensZoomRW(geoTabRW, MGeoTabS.mapInit)( MMapInitStateFastEq )
  private[sc] val mmapsRW         = mkLensZoomRW(mapInitRW, MMapInitState.state)( MMapSFastEq4Map )
  private[sc] val searchTextRW    = mkLensZoomRW(searchRW, MScSearch.text)( MScSearchTextFastEq )
  private[sc] val geoTabDataRW    = mkLensZoomRW(geoTabRW, MGeoTabS.data)( MGeoTabData.MGeoTabDataFastEq )
  private[sc] val mapDelayRW      = mkLensZoomRW(geoTabDataRW, MGeoTabData.delay)( OptFastEq.Wrapped(MMapDelay.MMapDelayFastEq) )

  private[sc] val gridRW          = mkLensRootZoomRW(this, MScRoot.grid)( MGridSFastEq )

  private[sc] val devRW           = mkLensRootZoomRW(this, MScRoot.dev)( MScDevFastEq )
  private[sc] val scScreenRW      = mkLensZoomRW(devRW, MScDev.screen)( MScScreenSFastEq )
  private[sc] val scGeoLocRW      = mkLensZoomRW(devRW, MScDev.geoLoc)( MScGeoLocFastEq )

  private[sc] val confRO          = mkLensZoomRO(internalsRW, MScInternals.conf)( MSc3Conf.MSc3ConfFastEq )
  private[sc] val rcvrsMapUrlRO   = mkLensZoomRO(confRO, MSc3Conf.rcvrsMapUrl)( FastEq.AnyRefEq )

  private[sc] val platformRW      = mkLensZoomRW(devRW, MScDev.platform)( MPlatformSFastEq )

  private[sc] val beaconerRW      = mkLensZoomRW(devRW, MScDev.beaconer)( MBeaconerSFastEq )

  private[sc] val dialogsRW       = mkLensRootZoomRW(this, MScRoot.dialogs )( MScDialogsFastEq )
  private[sc] val firstRunDiaRW   = mkLensZoomRW(dialogsRW, MScDialogs.first)( OptFastEq.Wrapped(MWzFirstOuterSFastEq) )

  private[sc] val bootRW          = mkLensZoomRW(internalsRW, MScInternals.boot)( MScBootFastEq )


  private[sc] def getLocEnv(mroot: MScRoot = rootRW.value, currRcvrId: Option[_] = None): MLocEnv = {
    MLocEnv(
      geoLocOpt  = OptionUtil.maybeOpt(currRcvrId.isEmpty)( mroot.geoLocOpt ),
      bleBeacons = mroot.locEnvBleBeacons
    )
  }


  /** Функция для сборки контекста рендера sc-шаблонов. */
  val scReactContextRO = zoom { mroot =>
    MScReactCtx(
      scCss     = mroot.index.scCss,
      routerCtl = routerCtl,
    )
  }


  /** Модель аргументов для поиска новых карточек в плитке. */
  private val gridAdsQsRO: ModelRO[MScQs] = zoom { mroot =>
    val inxState = mroot.index.state

    // TODO Унести сборку этого qs в контроллер или в утиль? Тут используется currRcvrId:
    // nodeId ресивера может быть задан в switchCtx, который известен только в контроллере, и отличаться от значения currRcvrId.
    val currRcvrId = inxState.rcvrId.toEsUuIdOpt

    MScQs(
      common = MScCommonQs(
        apiVsn = mroot.internals.conf.apiVsn,
        screen = Some {
          val scr0 = mroot.dev.screen.info.safeScreen
          // 2018-01-24 Костыль в связи с расхождением между szMult экрана и szMult плитки, тут быстрофикс:
          val pxRatio2 = MPxRatios.forRatio(
            Math.max(
              mroot.grid.core.jdConf.szMult.toDouble,
              scr0.pxRatio.pixelRatio
            )
          )
          if (pxRatio2.value > scr0.pxRatio.value)
            scr0.withPxRatio( pxRatio2 )
          else
            scr0
        },
        locEnv = getLocEnv(mroot, currRcvrId)
      ),
      search = MAdsSearchReq(
        rcvrId      = currRcvrId,
        genOpt      = Some( inxState.generation ),
        tagNodeId   = mroot.index.search.geo.data.selTagIds
          .headOption      // TODO Научить сервер поддерживать несколько тегов одновременно.
          .toEsUuIdOpt
        // limit и offset очень специфичны и выставляются в конкретных контроллерах карточек.
      )
    )
  }

  /** Аргументы для поиска тегов. */
  private val geoSearchQsRO: ModelRO[MScQs] = zoom { mroot =>
    _searchQs(mroot)
  }

  private def _searchQs(mroot: MScRoot): MScQs = {
    MScQs(
      common = MScCommonQs(
        locEnv      = getLocEnv(mroot),
        apiVsn      = mroot.internals.conf.apiVsn,
        searchNodes = Some( false ),
        screen      = Some( mroot.dev.screen.info.screen )
      ),
      search = MAdsSearchReq(
        textQuery = mroot.index.search.text.searchQuery.toOption,
        rcvrId    = mroot.index.state.rcvrId.toEsUuIdOpt
      )
    )
  }


  private val screenInfoRO  = mkLensZoomRO(scScreenRW, MScScreenS.info)( MScreenInfoFastEq )
  private val screenRO      = mkLensZoomRO(screenInfoRO, MScreenInfo.screen)( MScreenFastEq )


  /** Списки обработчиков ответов ScUniApi с сервера и resp-action в этих ответах. */
  val (respHandlers, respActionHandlers) = {
    // Часть модулей является универсальной, поэтому шарим хвост списка между обоими списками:
    val mixed = List[IRespWithActionHandler](
      new GridRespHandler,
      new GridFocusRespHandler,
      new IndexRah,
      new NodesSearchRespHandler( screenInfoRO ),
    )

    val rahs: List[IRespActionHandler] =
      new ConfUpdateRah ::
      mixed

    val rhs: List[IRespHandler] =
      mixed

    (rhs, rahs)
  }


  // Колелкцияaction-handler'ов:

  // хвостовой контроллер -- в самом конце, когда остальные отказались обрабатывать сообщение.
  private val tailAh = new TailAh(
    modelRW               = rootRW,
    routerCtl             = routerCtl,
    scRespHandlers        = respHandlers,
    scRespActionHandlers  = respActionHandlers,
  )

  private val geoTabAh = new GeoTabAh(
    modelRW         = geoTabRW,
    api             = api,
    geoSearchQsRO   = geoSearchQsRO,
    rcvrsMapApi     = advRcvrsMapApi,
    screenInfoRO    = screenInfoRO,
    rcvrMapArgsRO   = rcvrsMapUrlRO,
  )

  private val indexAh = new IndexAh(
    api     = api,
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

  private val gridAdsAh = new GridAh(
    api           = api,
    scQsRO        = gridAdsQsRO,
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

  private val firstRunDialogAh = new FirstRunDialogAh(
    platformRO  = platformRW,
    modelRW     = firstRunDiaRW,
    dispatcher  = this,
  )

  private val bootAh = new BootAh(
    modelRW = bootRW,
    circuit = this,
  )


  private def advRcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl( ScJsRoutes )

  override protected val actionHandler: HandlerFunction = {
    // TODO На основе конкретного Action-интерфейса роутить сигнал сразу к нужному контроллеру.
    var acc = List.empty[HandlerFunction]

    // TODO Opt Здесь много вызовов model.value. Может быть эффективнее будет один раз прочитать всю модель, и сверять её разные поля по мере необходимости?

    // В самый хвост списка добавить дефолтовый обработчик для редких событий и событий, которые можно дропать.
    acc ::= tailAh

    // Диалоги обычно закрыты. Тоже - в хвост.
    acc ::= firstRunDialogAh

    // Контроллер загрузки
    acc ::= bootAh

    // Листенер инициализации роутера. Выкидывать его после окончания инициализации.
    //if ( !jsRouterRW.value.isReady ) {
      acc ::= new JsRouterInitAh(
        circuit = circuit,
        modelRW = internalsRW
      )
    //}

    // События уровня платформы.
    acc ::= platformAh


    acc ::= sTextAh
    acc ::= geoTabAh    // TODO Объеденить с searchAh

    // Основные события индекса не частые, но доступны всегда *ДО*geoTabAh*:
    acc ::= indexAh

    //if ( indexWelcomeRW().nonEmpty )
      acc ::= new WelcomeAh( indexWelcomeRW )

    //if ( mapInitRW.value.ready )
      acc ::= mapAhs

    // Контроллеры СНАЧАЛА экрана, а ПОТОМ плитки. Нужно соблюдать порядок.
    acc ::= gridAdsAh

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


  // Отработать инициализацию js-роутера в самом начале конструктора.
  // По факту, инициализация уже наверное запущена в main(), но тут ещё и подписка на события...
  {
    // Немедленный запуск инициализации/загрузки
    Try {
      dispatch(
        Boot( MBootServiceIds.RcvrsMap :: Nil )
      )
    }

    //  Когда наступает platform ready и BLE доступен, надо попробовать активировать/выключить слушалку маячков BLE и разрешить геолокацию.
    def __dispatchBleBeaconerOnOff(): Unit = {
      try {
        val plat = platformRW.value
        if (plat.hasBle && plat.isReady) {
          //LOG.warn( "ok, dispatching ble on/off", msg = plat )
          Future {
            val msg = BtOnOff( isEnabled = plat.isUsingNow)
            dispatch( msg )
          }
        }
      } catch {
        case ex: Throwable =>
          LOG.error( ErrorMsgs.CORDOVA_BLE_REQUIRE_FAILED, ex )
      }
    }

    // TODO Platform boot - унесено в BootAh.PlatformSvc
    val isPlatformReadyRO = platformRW.zoom(_.isReady)
    // Начинаем юзать платформу прямо в конструкторе circuit. Это может быть небезопасно, поэтому тут try-catch для всей этой логики.
    try {
      // Лезть в состояние на стадии конструктора - плохая примета. Поэтому защищаемся от возможных косяков в будущем через try-обёртку вокруг zoom.value()
      if ( Try(isPlatformReadyRO.value).getOrElse(false) ) {
        // Платформа уже готова. Запустить эффект активации BLE-маячков.
        //LOG.log( msg = isPlatformReadyNowTry )
        __dispatchBleBeaconerOnOff()
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
            __dispatchBleBeaconerOnOff()
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
        (scalajs.LinkingInfo.developmentMode || !mgl.switch.hardLock)
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
    subscribe( platformRW.zoom(_.isUsingNow) ) { isUsingNowProxy =>
      // Отключать мониторинг BLE-маячков, когда платформа позволяет это делать.
      __dispatchBleBeaconerOnOff()

      // Глушить фоновый GPS-мониторинг:
      val isUsingNow = isUsingNowProxy.value
      __dispatchGeoLocOnOff(isUsingNow)
    }

    // Подписаться на события изменения списка наблюдаемых маячков.
    // TODO Opt Не подписываться без необходимости.
    subscribe( beaconerRW.zoom(_.nearbyReport) ) { _ =>
      //println( "beacons changed: " + nearbyReportProxy.value.mkString("\n[", ",\n", "\n]") )
      val mroot = rootRW.value
      if (mroot.index.resp.isPending) {
        LOG.log( WarnMsgs.SUPPRESSED_INSUFFICIENT, msg = "ble!inx" )
        // TODO Если сигнал пришёл, когда уже идёт запрос плитки/индекса, то надо это уведомление отправлять в очередь?

      } else {
        // Надо запустить пересборку плитки. Без Future, т.к. это - callback-функция.
        val action = GridLoadAds(
          clean         = true,
          ignorePending = true,
          silent        = Some(true)
        )
        dispatch( action )
      }
    }

  }

}
