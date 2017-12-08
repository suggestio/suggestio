package io.suggest.sc

import diode.ModelRO
import diode.react.ReactConnector
import io.suggest.common.event.WndEvents
import io.suggest.dev.JsScreenUtil
import io.suggest.geo.MLocEnv
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.maps.c.{MapCommonAh, RcvrMarkersInitAh}
import io.suggest.maps.m.{MMapS, RcvrMarkersInit}
import io.suggest.routes.{AdvRcvrsMapApiHttp, scRoutes}
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sc.c.dev.{GeoLocAh, ScreenAh}
import io.suggest.sc.c.TailAh
import io.suggest.sc.grid.c.GridAdsAh
import io.suggest.sc.grid.m.MGridS
import io.suggest.sc.inx.c.{IndexAh, IndexMapAh, WelcomeAh}
import io.suggest.sc.inx.m.MScIndex
import io.suggest.sc.m.{JsRouterInit, MScRoot, ScreenReset}
import io.suggest.sc.m.dev.{MScDev, MScScreenS}
import io.suggest.sc.router.c.JsRouterInitAh
import io.suggest.sc.sc3.MSc3Init
import io.suggest.sc.search.c.{STextAh, SearchAh, TagsAh}
import io.suggest.sc.search.m.{MMapInitState, MScSearch}
import io.suggest.sc.styl.{ScCss, ScCssFactory}
import io.suggest.sc.tags.MScTagsSearchQs
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.spa.StateInp
import org.scalajs.dom.Event
import play.api.libs.json.Json

import scala.concurrent.{Future, Promise}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:00
  * Description: Main circuit новой выдачи. Отрабатывает весь интерфейс выдачи v3.
  */
class Sc3Circuit(
                  scCssFactoryModule    : ScCssFactory,
                  jdCssFactory          : JdCssFactory,
                  api                   : ISc3Api,
                  getRouterCtlF         : GetRouterCtlF,
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
  import inx.m.MWelcomeState.MWelcomeStateFastEq
  import styl.MScCssArgs.MScCssArgsFastEq

  import MScSearch.MScSearchFastEq
  import search.m.MScSearchText.MScSearchTextFastEq
  import MScRoot.MScRootFastEq
  import MMapInitState.MMapInitStateFastEq


  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  override protected def initialModel: MScRoot = {
    // TODO Десериализовать состояние из URL или откуда-нибудь ещё.
    val state0 = Json
      .parse( StateInp.find().get.value.get )
      .as[MSc3Init]

    val mscreen = JsScreenUtil.getScreen()

    MScRoot(
      dev = MScDev(
        screen = MScScreenS(
          screen = mscreen
        )
      ),
      index = MScIndex(
        search = MScSearch(
          mapInit = MMapInitState(
            state = MMapS(state0.mapProps)
          )
        )
      ),
      grid = {
        val jdConf = GridAdsAh.fullGridConf(mscreen)
        MGridS(
          jdConf = jdConf,
          jdCss  = jdCssFactory.mkJdCss( MJdCssArgs(conf = jdConf) )
        )
      }
    )
  }


  // Кэш zoom'ов модели:
  private val rootRW = zoomRW(m => m) { (_, new2) => new2 }

  private val internalsRW = zoomRW(_.internals) { _.withInternals(_) }
  private val jsRouterRW = internalsRW.zoomRW(_.jsRouter) { _.withJsRouter(_) }

  private val indexRW = zoomRW(_.index) { _.withIndex(_) }
  private val indexWelcomeRW = indexRW.zoomRW(_.welcome) { _.withWelcome(_) }
  //private val indexStateRW = indexRW.zoomRW(_.state) { _.withState(_) }

  private val searchRW = indexRW.zoomRW(_.search) { _.withSearch(_) }
  private val tagsRW = searchRW.zoomRW(_.tags) { _.withTags(_) }

  private val mapInitRW = searchRW.zoomRW(_.mapInit) { _.withMapInit(_) }
  private val searchMapRcvrsPotRW = mapInitRW.zoomRW(_.rcvrsGeo) { _.withRcvrsGeo(_) }
  private val mmapsRW = mapInitRW.zoomRW(_.state) { _.withState(_) }
  private val searchTextRW = searchRW.zoomRW(_.text) { _.withText(_) }

  private val gridRW = zoomRW(_.grid) { _.withGrid(_) }

  private val devRW = zoomRW(_.dev) { _.withDev(_) }
  private val scScreenRW = devRW.zoomRW(_.screen) { _.withScreen(_) }
  private val scGeoLocRW = devRW.zoomRW(_.geoLoc) { _.withGeoLoc(_) }


  private val searchAdsArgsRO: ModelRO[MFindAdsReq] = zoom { mroot =>
    val inxState = mroot.index.state
    val currRcvrId = inxState.currRcvrId
    MFindAdsReq(
      receiverId  = currRcvrId,
      locEnv      = if (currRcvrId.isEmpty) mroot.locEnv else MLocEnv.empty,
      screenInfo  = Some( mroot.dev.screen.screen ),
      generation  = Some( inxState.generation ),
      tagNodeId   = mroot.index.search.tags.selectedId
      // limit и offset очень специфичны и выставляются в конкретных контроллерах карточек.
    )
  }

  private val tagsSearchArgsQsRO: ModelRO[MScTagsSearchQs] = zoom { mroot =>
    val currRcvrId = mroot.index.state.currRcvrId
    MScTagsSearchQs(
      tagsQuery = mroot.index.search.text.searchQuery.toOption,
      locEnv    = if (currRcvrId.isEmpty) mroot.locEnv else MLocEnv.empty,
      rcvrId    = currRcvrId,
      apiVsn    = Sc3Api.API_VSN
    )
  }

  private val screenRO = scScreenRW.zoom(_.screen)


  // Кэш action-handler'ов

  // хвостовой контроллер -- в самом конце, когда остальные отказались обрабатывать сообщение.
  private val tailAh = new TailAh(
    modelRW     = rootRW,
    routerCtlF  = getRouterCtlF
  )

  private val searchAh = new SearchAh(
    modelRW       = searchRW
  )

  private lazy val tagsAh = new TagsAh(
    api           = api,
    modelRW       = tagsRW,
    searchArgsRO  = tagsSearchArgsQsRO,
    screenRO      = screenRO
  )

  private val indexAh = new IndexAh(
    api     = api,
    modelRW = indexRW,
    stateRO = rootRW
  )

  private lazy val mapAndIndexAh = {
    val mapCommonAh = new MapCommonAh(
      mmapRW = mmapsRW
    )
    val indexMapAh = new IndexMapAh(
      modelRW = indexRW
    )
    foldHandlers( mapCommonAh, indexMapAh )
  }

  private val gridAdsAh = new GridAdsAh(
    api           = api,
    searchArgsRO  = searchAdsArgsRO,
    screenRO      = screenRO,
    jdCssFactory  = jdCssFactory,
    modelRW       = gridRW
  )

  private val screenAh = new ScreenAh(
    modelRW = scScreenRW
  )

  private val sTextAh = new STextAh(
    modelRW = searchTextRW
  )

  private val geoLocAh = new GeoLocAh(
    dispatcher  = this,
    modelRW     = scGeoLocRW
  )


  private def advRcvrsMapApi = new AdvRcvrsMapApiHttp( scRoutes )

  override protected def actionHandler: HandlerFunction = {
    var acc = List.empty[HandlerFunction]

    // TODO Opt Здесь много вызовов model.value. Может быть эффективнее будет один раз прочитать всю модель, и сверять её разные поля по мере необходимости?

    // В самый хвост списка добавить дефолтовый обработчик для редких событий и событий, которые можно дропать.
    acc ::= tailAh

    // Листенер инициализации роутера. Выкидывать его после окончания инициализации.
    if ( !jsRouterRW.value.isReady ) {
      acc ::= new JsRouterInitAh(
        circuit = circuit,
        modelRW = jsRouterRW
      )
    }

    // Основные события индекса не частые, но доступны всегда:
    acc ::= indexAh

    // Инициализатор карты ресиверов на гео-карте.
    if ( !searchMapRcvrsPotRW.value.isReady )
      acc ::= new RcvrMarkersInitAh( advRcvrsMapApi, searchMapRcvrsPotRW )

    // top-level search AH всегда ожидает команд, когда TODO нет открытого левого меню закрыто или focused-выдачи
    acc ::= searchAh

    // TODO Opt sTextAh не нужен, когда панель поиска скрыта.
    acc ::= sTextAh

    if ( indexWelcomeRW().nonEmpty )
      acc ::= new WelcomeAh( indexWelcomeRW )

    if ( mapInitRW.value.ready )
      acc ::= mapAndIndexAh

    // Контроллеры СНАЧАЛА экрана, а ПОТОМ плитки. Нужно соблюдать порядок.
    acc ::= gridAdsAh

    val searchS = searchRW.value
    if (searchS.isTagsVisible)
      acc ::= tagsAh

    // Геолокация довольно часто получает сообщения (когда активна), поэтому её -- тоже в начало списка контроллеров:
    acc ::= geoLocAh

    // Экран отрабатываем в начале, но необходимость этого под вопросом.
    acc ::= screenAh

    // Собрать все контроллеры в пачку.
    composeHandlers( acc: _* )
  }


  // Отработать инициализацию роутера в самом начале конструктора.
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

  }


  /** Зуммер для получения инстанса динамических аргументов рендера ScCss. */
  private val scCssArgsRO = zoom(_.scCssArgs)

  private var _scCssCacheOpt: Option[ScCss] = None

  // Постоянная подписка на события реальных изменений, связанных со стилями ScCss:
  subscribe(scCssArgsRO) { _ =>
    // Изменились какие-то параметры, связанные со стилями. Просто сбросить кэш ScCss:
    _scCssCacheOpt = None
  }

  def scCss(): ScCss = {
    _scCssCacheOpt.getOrElse {
      // Заполнить кэш ScCss согласно текущим параметрам рендера:
      synchronized {
        val scCss = scCssFactoryModule.mkScCss( scCssArgsRO() )
        _scCssCacheOpt = Some( scCss )
        scCss
      }
    }
  }

}
