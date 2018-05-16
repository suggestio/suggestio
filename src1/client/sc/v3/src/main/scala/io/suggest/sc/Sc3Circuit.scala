package io.suggest.sc

import diode.ModelRO
import diode.data.Pot
import diode.react.ReactConnector
import io.suggest.common.event.WndEvents
import io.suggest.dev.JsScreenUtil
import io.suggest.geo.MLocEnv
import io.suggest.i18n.MsgCodes
import io.suggest.jd.render.m.MJdCssArgs
import io.suggest.jd.render.v.JdCssFactory
import io.suggest.maps.c.{MapCommonAh, RcvrMarkersInitAh}
import io.suggest.maps.m.{MMapS, RcvrMarkersInit}
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.routes.AdvRcvrsMapApiHttpViaUrl
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sc.c.dev.{GeoLocAh, ScreenAh}
import io.suggest.sc.c.{JsRouterInitAh, TailAh}
import io.suggest.sc.c.grid.GridAdsAh
import io.suggest.sc.c.inx.{IndexAh, WelcomeAh}
import io.suggest.sc.c.menu.MenuAh
import io.suggest.sc.c.search.{STextAh, ScMapDelayAh, SearchAh, TagsAh}
import io.suggest.sc.m._
import io.suggest.sc.m.dev.{MScDev, MScScreenS}
import io.suggest.sc.m.grid.MGridS
import io.suggest.sc.m.inx.MScIndex
import io.suggest.sc.m.search.{MMapInitState, MScSearch}
import io.suggest.sc.sc3.{MSc3IndexResp, MSc3Init}
import io.suggest.sc.styl.{MScCssArgs, ScCss}
import io.suggest.sc.tags.MScTagsSearchQs
import io.suggest.sc.v.ScCssFactory
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.spa.{OptFastEq, StateInp}
import org.scalajs.dom
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
                  scCssFactory          : ScCssFactory,
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
  import m.inx.MWelcomeState.MWelcomeStateFastEq

  import MScSearch.MScSearchFastEq
  import m.search.MScSearchText.MScSearchTextFastEq
  import MScRoot.MScRootFastEq
  import MMapInitState.MMapInitStateFastEq


  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  override protected def initialModel: MScRoot = {
    // TODO Десериализовать состояние из URL или откуда-нибудь ещё.
    val scInit = Json
      .parse( StateInp.find().get.value.get )
      .as[MSc3Init]

    val mscreen = JsScreenUtil.getScreen()

    val scIndexResp = Pot.empty[MSc3IndexResp]

    MScRoot(
      dev = MScDev(
        screen = MScScreenS(
          screen = mscreen
        )
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
        val jdConf = GridAdsAh.fullGridConf(mscreen)
        MGridS(
          jdConf = jdConf,
          jdCss  = jdCssFactory.mkJdCss( MJdCssArgs(conf = jdConf) )
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


  private val searchAdsArgsRO: ModelRO[MFindAdsReq] = zoom { mroot =>
    val inxState = mroot.index.state
    val currRcvrId = inxState.currRcvrId
    MFindAdsReq(
      receiverId  = currRcvrId,
      locEnv      = if (currRcvrId.isEmpty) mroot.locEnv else MLocEnv.empty,
      screenInfo  = Some {
        val scr0 = mroot.dev.screen.screen
        // 2018-01-24 Костыль в связи с расхождением между szMult экрана и szMult плитки, тут быстрофикс:
        val pxRatio2 = Math.max(
          mroot.grid.jdConf.szMult.toDouble,
          scr0.pxRatio
        )
        if (pxRatio2 > scr0.pxRatio)
          scr0.withPxRatio( pxRatio2 )
        else
          scr0
      },
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

  private val tagsAh = new TagsAh(
    api           = api,
    modelRW       = tagsRW,
    searchArgsRO  = tagsSearchArgsQsRO,
    screenRO      = screenRO
  )

  private val indexAh = new IndexAh(
    api     = api,
    modelRW = indexRW,
    rootRO = rootRW,
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

  private val menuAh = new MenuAh(
    modelRW = menuRW
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

    // Основные события индекса не частые, но доступны всегда:
    acc ::= indexAh

    // Инициализатор карты ресиверов на гео-карте.
    //if ( !searchMapRcvrsPotRW.value.isReady )
      acc ::= new RcvrMarkersInitAh( advRcvrsMapApi, searchMapRcvrsPotRW )

    // top-level search AH всегда ожидает команд, когда TODO нет открытого левого меню закрыто или focused-выдачи
    acc ::= searchAh

    // TODO Opt sTextAh не нужен, когда панель поиска скрыта.
    acc ::= sTextAh

    //if ( indexWelcomeRW().nonEmpty )
      acc ::= new WelcomeAh( indexWelcomeRW )

    //if ( mapInitRW.value.ready )
      acc ::= mapAhs

    // Контроллеры СНАЧАЛА экрана, а ПОТОМ плитки. Нужно соблюдать порядок.
    acc ::= gridAdsAh

    //val searchS = searchRW.value
    //if (searchS.isTagsVisible)
      acc ::= tagsAh

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

  }

}
