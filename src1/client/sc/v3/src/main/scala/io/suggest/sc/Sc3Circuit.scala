package io.suggest.sc

import diode.ModelRO
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.ble.beaconer.c.BleBeaconerAh
import io.suggest.ble.beaconer.m.BbOnOff
import io.suggest.common.event.WndEvents
import io.suggest.dev.{JsScreenUtil, MPxRatios}
import io.suggest.es.model.MEsUuId
import io.suggest.geo.MLocEnv
import io.suggest.i18n.MsgCodes
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.maps.c.{MapCommonAh, RcvrMarkersInitAh}
import io.suggest.maps.m.{MMapS, RcvrMarkersInit}
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.routes.AdvRcvrsMapApiHttpViaUrl
import io.suggest.sc.ads.MAdsSearchReq
import io.suggest.sc.c.dev.{GeoLocAh, PlatformAh, ScreenAh}
import io.suggest.sc.c.{IRespWithActionHandler, JsRouterInitAh, TailAh}
import io.suggest.sc.c.grid.GridAh
import io.suggest.sc.c.inx.{IndexAh, WelcomeAh}
import io.suggest.sc.c.menu.MenuAh
import io.suggest.sc.c.search.{STextAh, ScMapDelayAh, SearchAh, TagsAh}
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.m._
import io.suggest.sc.m.dev.{MScDev, MScScreenS}
import io.suggest.sc.m.grid.{GridLoadAds, MGridCoreS, MGridS}
import io.suggest.sc.m.inx.MScIndex
import io.suggest.sc.m.search.{MMapInitState, MScSearch}
import io.suggest.sc.sc3.{MScCommonQs, MScQs}
import io.suggest.sc.styl.{MScCssArgs, ScCss}
import io.suggest.sc.u.Sc3ConfUtil
import io.suggest.sc.v.ScCssFactory
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.spa.OptFastEq
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:00
  * Description: Main circuit новой выдачи. Отрабатывает весь интерфейс выдачи v3.
  */
class Sc3Circuit(
                  scCssFactory              : ScCssFactory,
                  jdCssFactory              : JdCssFactory,
                  api                       : ISc3Api,
                  getRouterCtlF             : GetRouterCtlF,
                  respWithActionHandlers    : Seq[IRespWithActionHandler],
                )
  extends CircuitLog[MScRoot]
  with ReactConnector[MScRoot]
{ circuit =>

  import MScIndex.MScIndexFastEq
  import m.MScInternals.MScInternalsFastEq
  import MGridS.MGridSFastEq
  import MScDev.MScDevFastEq
  import MScScreenS.MScScreenSFastEq
  import m.dev.MScGeoLoc.MScGeoFastEq
  import m.inx.MWelcomeState.MWelcomeStateFastEq

  import MScSearch.MScSearchFastEq
  import m.search.MScSearchText.MScSearchTextFastEq
  import MScRoot.MScRootFastEq
  import MMapInitState.MMapInitStateFastEq

  import MEsUuId.Implicits._
  import io.suggest.sc.m.dev.MPlatformS.MPlatformSFastEq
  import io.suggest.ble.beaconer.m.MBeaconerS.MBeaconerSFastEq


  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  override protected def initialModel: MScRoot = {
    // Сначала надо подготовить конфиг, прочитав его со страницы (если он там есть).
    val scInit = Sc3ConfUtil.initFromDom()
      .getOrElse {
        // TODO Нужен какой-то авто-конфиг. С сервера надо получать разные полезные данные, запихивать результат в circuit-конструктор.
        val emsg = ErrorMsgs.GRID_CONFIGURATION_INVALID
        LOG.error( emsg )
        throw new NoSuchElementException( emsg )
      }

    val mscreen = JsScreenUtil.getScreen()
    val scIndexResp = Pot.empty[MSc3IndexResp]

    MScRoot(
      dev = MScDev(
        screen = MScScreenS(
          screen = mscreen
        ),
        platform = PlatformAh.platformInit(this)
      ),
      index = MScIndex(
        resp = scIndexResp,
        search = MScSearch(
          mapInit = MMapInitState(
            state = MMapS(scInit.mapProps)
          )
        ),
        scCss = scCssFactory.mkScCss(
          MScCssArgs.from(scIndexResp, mscreen)
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
            jdCss  = jdCssFactory.mkJdCss( MJdCssArgs(conf = jdConf) )
          )
        )
      },
      internals = MScInternals(
        conf = scInit.conf
      )
    )
  }


  // Кэш zoom'ов модели:
  private val rootRW = zoomRW(m => m) { (_, new2) => new2 }

  private val internalsRW = zoomRW(_.internals) { _.withInternals(_) }
  private val jsRouterRW = internalsRW.zoomRW(_.jsRouter) { _.withJsRouter(_) }

  private val indexRW = zoomRW(_.index) { _.withIndex(_) }
  private val titleOptRO = indexRW.zoom( _.resp.toOption.flatMap(_.name) )( OptFastEq.Plain )
  private val indexWelcomeRW = indexRW.zoomRW(_.welcome) { _.withWelcome(_) }
  //private val indexStateRW = indexRW.zoomRW(_.state) { _.withState(_) }

  val scCssRO: ModelRO[ScCss] = indexRW.zoom(_.scCss)

  private val searchRW = indexRW.zoomRW(_.search) { _.withSearch(_) }
  private val tagsRW = searchRW.zoomRW(_.tags) { _.withTags(_) }

  private val mapInitRW = searchRW.zoomRW(_.mapInit) { _.withMapInit(_) }
  private val searchMapRcvrsPotRW = mapInitRW.zoomRW(_.rcvrsGeo) { _.withRcvrsGeo(_) }
  private val mmapsRW = mapInitRW.zoomRW(_.state) { _.withState(_) }
  private val searchTextRW = searchRW.zoomRW(_.text) { _.withText(_) }
  private val mapDelayRW = mapInitRW.zoomRW(_.delay) { _.withDelay(_) }

  private val gridRW = zoomRW(_.grid) { _.withGrid(_) }

  private val devRW = zoomRW(_.dev) { _.withDev(_) }
  private val scScreenRW = devRW.zoomRW(_.screen) { _.withScreen(_) }
  private val scGeoLocRW = devRW.zoomRW(_.geoLoc) { _.withGeoLoc(_) }

  private val confRO = internalsRW.zoom(_.conf)

  private val menuRW = indexRW.zoomRW(_.menu) { _.withMenu(_) }

  private val platformRW = devRW.zoomRW(_.platform) { _.withPlatform(_) }
  private val beaconerRW = devRW.zoomRW(_.beaconer) { _.withBeaconer(_) }


  private val gridAdsQsRO: ModelRO[MScQs] = zoom { mroot =>
    val inxState = mroot.index.state
    val currRcvrId = inxState.currRcvrId.toEsUuIdOpt
    MScQs(
      common = MScCommonQs(
        apiVsn = mroot.internals.conf.apiVsn,
        screen = Some {
          val scr0 = mroot.dev.screen.screen
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
        locEnv = {
          if (currRcvrId.isEmpty) mroot.locEnv
          else MLocEnv.empty
        }
      ),
      search = MAdsSearchReq(
        rcvrId      = currRcvrId,
        genOpt      = Some( inxState.generation ),
        tagNodeId   = mroot.index.search.tags.selectedId.toEsUuIdOpt
        // limit и offset очень специфичны и выставляются в конкретных контроллерах карточек.
      )
    )
  }

  /** Аргументы для поиска тегов. */
  private val tagsSearchQsRO: ModelRO[MScQs] = zoom { mroot =>
    val currRcvrId = mroot.index.state.currRcvrId
    MScQs(
      common = MScCommonQs(
        locEnv =
          if (currRcvrId.isEmpty) mroot.locEnv
          else MLocEnv.empty,
        apiVsn = mroot.internals.conf.apiVsn,
        searchTags = Some(false)
      ),
      search = MAdsSearchReq(
        textQuery = mroot.index.search.text.searchQuery.toOption,
        rcvrId    = currRcvrId.toEsUuIdOpt
      )
    )
  }

  private val screenRO = scScreenRW.zoom(_.screen)


  // Кэш action-handler'ов

  // хвостовой контроллер -- в самом конце, когда остальные отказались обрабатывать сообщение.
  private val tailAh = new TailAh(
    modelRW     = rootRW,
    routerCtlF  = getRouterCtlF,
    respWithActionHandlers = respWithActionHandlers,
  )

  private val searchAh = new SearchAh(
    modelRW       = searchRW
  )

  private val tagsAh = new TagsAh(
    api             = api,
    modelRW         = tagsRW,
    tagsSearchQsRO  = tagsSearchQsRO,
    screenRO        = screenRO
  )

  private val indexAh = new IndexAh(
    api     = api,
    modelRW = indexRW,
    rootRO  = rootRW,
    scCssFactory = scCssFactory
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
    jdCssFactory  = jdCssFactory,
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

  private val menuAh = new MenuAh(
    modelRW = menuRW
  )

  private val platformAh = new PlatformAh(
    modelRW = platformRW
  )

  private val beaconerAh = new BleBeaconerAh(
    modelRW     = beaconerRW,
    dispatcher  = this
  )


  private def advRcvrsMapApi = new AdvRcvrsMapApiHttpViaUrl( confRO.value.rcvrsMapUrl )

  override protected val actionHandler: HandlerFunction = {
    var acc = List.empty[HandlerFunction]

    // TODO Opt Здесь много вызовов model.value. Может быть эффективнее будет один раз прочитать всю модель, и сверять её разные поля по мере необходимости?

    // В самый хвост списка добавить дефолтовый обработчик для редких событий и событий, которые можно дропать.
    acc ::= tailAh

    // Листенер инициализации роутера. Выкидывать его после окончания инициализации.
    //if ( !jsRouterRW.value.isReady ) {
      acc ::= new JsRouterInitAh(
        circuit = circuit,
        modelRW = internalsRW
      )
    //}

    // Менюшка. По идее, используется не чаще, чем index.
    acc ::= menuAh

    // События уровня платформы.
    acc ::= platformAh

    // Основные события индекса не частые, но доступны всегда:
    acc ::= indexAh

    // Инициализатор карты ресиверов на гео-карте.
    //if ( !searchMapRcvrsPotRW.value.isReady )
      acc ::= new RcvrMarkersInitAh( advRcvrsMapApi, searchMapRcvrsPotRW )

    // top-level search AH всегда ожидает команд, когда TODO нет открытого левого меню закрыто или focused-выдачи
    acc ::= searchAh

    // TODO Opt sTextAh не нужен, когда панель поиска скрыта.
    acc ::= sTextAh

    //val searchS = searchRW.value
    //if (searchS.isTagsVisible)
      acc ::= tagsAh

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


  // Отработать инициализацию js-роутера в самом начале конструктора.
  // По факту, инициализация уже наверное запущена в main(), но тут ещё и подписка на события...
  {
    dispatch( JsRouterInit )

    val jsRouterReadyP = Promise[None.type]()
    val unSubscribeJsRouterF = subscribe( jsRouterRW ) { jsRouterPotProxy =>
      if (jsRouterPotProxy.value.nonEmpty) {
        // Запустить инициализацию начального индекса выдачи.
        try {
          // Запустить получения гео-маркеров с сервера.
          if (searchMapRcvrsPotRW.value.isEmpty) {
            Future {
              dispatch( RcvrMarkersInit )
            }
          }
          jsRouterReadyP.success( None )
        } catch {
          case ex: Throwable =>
            jsRouterReadyP.failure(ex)
        }
      }
    }
    // Удалить listener роутера можно только вызвав функцию, которую возвращает subscribe().
    jsRouterReadyP.future.onComplete { tryRes =>
      unSubscribeJsRouterF()
      for (ex <- tryRes.failed)
        LOG.error(ErrorMsgs.JS_ROUTER_INIT_FAILED, ex = ex)
    }

    // Подписаться на глобальные события window
    val wnd = WindowVm()
    val listenF = { _: Event => dispatch(ScreenReset) }
    for {
      evtName <- WndEvents.RESIZE :: WndEvents.ORIENTATION_CHANGE :: Nil
    } {
      try {
        wnd.addEventListener( evtName )( listenF )
      } catch {
        case ex: Throwable =>
          LOG.error( ErrorMsgs.EVENT_LISTENER_SUBSCRIBE_ERROR, ex, evtName )
      }
    }


    // Подписаться на обновление заголовка и обновлять заголовок.
    // Т.к. document.head.title -- это голая строка, то делаем рендер строки прямо здесь.
    subscribe( titleOptRO ) { titleOptRO =>
      val title0 = MsgCodes.`Suggest.io`
      val title1 = titleOptRO.value.fold(title0)(_ + " | " + title0)
      dom.document.title = title1
    }

    //  Когда наступает platform ready и BLE доступен, надо попробовать активировать/выключить слушалку маячков BLE и разрешить геолокацию.
    def __dispatchBleBeaconerOnOff() = {
      val plat = platformRW.value
      if (plat.isBleAvail && plat.isReady) {
        Future {
          val msg = BbOnOff( isEnabled = plat.isUsingNow)
          dispatch( msg )
        }
      }
    }

    // Дожидаться активности платформы, прежде чем заюзать её.
    val isPlatformReadyRO = platformRW.zoom(_.isReady)
    // Лезть в состояние на стадии конструктора - плохая примета. Поэтому защищаемся от возможных косяков в будущем через try-обёртку вокруг zoom.value()
    if ( Try(isPlatformReadyRO.value).getOrElse(false) ) {
      // Платформа уже готова. Запустить эффект активации BLE-маячков.
      __dispatchBleBeaconerOnOff()
    } else {
      // Платформа не готова. Значит, надо бы дождаться готовности платформы и повторить попытку.
      val sp = Promise[None.type]()
      val cancelF = subscribe(isPlatformReadyRO) { isReadyNowProxy =>
        if (isReadyNowProxy.value) {
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

    // Реагировать на события активности приложения выдачи.
    subscribe( platformRW.zoom(_.isUsingNow) ) { _ =>
      // Глушить BLE-маячки, когда платформа позволяет это делать.
      __dispatchBleBeaconerOnOff()
      // TODO Глушить фоновый GPS-мониторинг
    }

    // Подписаться на события изменения списка наблюдаемых маячков.
    // TODO Opt Не подписываться без необходимости.
    subscribe( beaconerRW.zoom(_.nearbyReport) ) { _ =>
      //println( "beacons changed: " + nearbyReportProxy.value.mkString("\n[", ",\n", "\n]") )
      // Надо запустить пересборку плитки.
      dispatch( GridLoadAds(clean = true, ignorePending = true) )
    }

  }

}
