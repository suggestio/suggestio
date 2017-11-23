package io.suggest.sc

import diode.ModelRO
import diode.react.ReactConnector
import io.suggest.common.event.WndEvents
import io.suggest.dev.JsScreenUtil
import io.suggest.geo.{MGeoPoint, MLocEnv}
import io.suggest.maps.c.{MapCommonAh, RcvrMarkersInitAh}
import io.suggest.maps.m.{MMapS, RcvrMarkersInit}
import io.suggest.routes.{AdvRcvrsMapApiHttp, scRoutes}
import io.suggest.sc.ads.MFindAdsReq
import io.suggest.sc.grid.c.GridAdsAh
import io.suggest.sc.init.MSc3Init
import io.suggest.sc.inx.c.{IndexAh, IndexStateAh, WelcomeAh}
import io.suggest.sc.inx.m.{GetIndex, MScIndex, MScIndexState}
import io.suggest.sc.m.ScreenReset
import io.suggest.sc.root.c.NoOpAh
import io.suggest.sc.root.m.{JsRouterStatus, MScRoot}
import io.suggest.sc.router.SrvRouter
import io.suggest.sc.router.c.JsRouterInitAh
import io.suggest.sc.search.c.SearchAh
import io.suggest.sc.search.m.MScSearch
import io.suggest.sc.styl.{ScCss, ScCssFactoryModule}
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.OptFastEq.Wrapped
import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.spa.StateInp
import org.scalajs.dom.Event
import play.api.libs.json.Json

import scala.concurrent.Promise

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:00
  * Description: Main circuit новой выдачи. Отрабатывает весь интерфейс выдачи v3.
  */
class Sc3Circuit(
                  scCssFactoryModule    : ScCssFactoryModule,
                  api                   : ISc3Api
                )
  extends CircuitLog[MScRoot]
  with ReactConnector[MScRoot]
{

  import MScIndex.MScIndexFastEq
  import io.suggest.sc.inx.m.MWelcomeState.MWelcomeStateFastEq
  import io.suggest.sc.styl.MScCssArgs.MScCssArgsFastEq

  import MScSearch.MScSearchFastEq


  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  override protected def initialModel = {
    // TODO Десериализовать состояние из URL или откуда-нибудь ещё.
    val state0 = Json
      .parse( StateInp.find().get.value.get )
      .as[MSc3Init]

    MScRoot(
      index = MScIndex(
        state = MScIndexState(
          screen   = JsScreenUtil.getScreen,
          geoPoint = Some( MGeoPoint(59.92, 30.31) )
        ),
        search = MScSearch(
          mapState = MMapS( state0.mapProps )
        )
      )
    )
  }


  // Кэш zoom'ов модели:
  private val jsRouterRW = zoomRW(_.jsRouter) { _.withJsRouter(_) }

  private val indexRW = zoomRW(_.index) { _.withIndex(_) }
  private val indexWelcomeRW = indexRW.zoomRW(_.welcome) { _.withWelcome(_) }
  private val indexStateRW = indexRW.zoomRW(_.state) { _.withState(_) }

  private val searchRW = indexRW.zoomRW(_.search) { _.withSearch(_) }
  private val searchMapRcvrsPotRW = searchRW.zoomRW(_.rcvrsGeo) { _.withRcvrsGeo(_) }
  private val mmapRW = searchRW.zoomRW(_.mapState) { _.withMapState(_) }

  private val gridRW = zoomRW(_.grid) { _.withGrid(_) }

  private val rootRO = zoom(m => m)

  private val searchAdsArgsRO: ModelRO[MFindAdsReq] = zoom { mroot =>
    val inxState = mroot.index.state
    MFindAdsReq(
      receiverId  = inxState.currRcvrId,
      locEnv      = if (inxState.currRcvrId.isEmpty) mroot.locEnv else MLocEnv.empty,
      screenInfo  = Some( inxState.screen ),
      generation  = Some( inxState.generation )
      // limit и offset очень специфичны и выставляются в конкретных контроллерах карточек.
      // TODO Добавить здесь tagNodeId.
    )
  }

  private val screenRO = indexStateRW.zoom(_.screen)


  // Кэш action-handler'ов

  private val noOpAh = new NoOpAh( jsRouterRW )

  private val searchAh = new SearchAh( modelRW = searchRW )

  private val indexAh = new IndexAh(
    api     = api,
    modelRW = indexRW,
    stateRO = rootRO
  )

  private val indexStateAh = new IndexStateAh(
    modelRW = indexStateRW
  )

  private lazy val mapCommonAh = new MapCommonAh(
    mmapRW = mmapRW
  )

  private val gridAdsAh = new GridAdsAh(
    api           = api,
    searchArgsRO  = searchAdsArgsRO,
    screenRO      = screenRO,
    modelRW       = gridRW
  )

  private def advRcvrsMapApi = new AdvRcvrsMapApiHttp( scRoutes )

  override protected def actionHandler: HandlerFunction = {
    var acc = List.empty[HandlerFunction]

    // В самый хвост списка добавить дефолтовый обработчик для некоторых событий, которые можно дропать.
    acc ::= noOpAh

    // Листенер инициализации роутера. Выкидывать его после окончания инициализации.
    if ( !jsRouterRW().isReady ) {
      acc ::= new JsRouterInitAh(
        modelRW = jsRouterRW
      )
    }

    // Инициализатор карты ресиверов на гео-карте.
    if ( !searchMapRcvrsPotRW.value.isReady )
      acc ::= new RcvrMarkersInitAh( advRcvrsMapApi, searchMapRcvrsPotRW )

    // top-level search AH всегда ожидает команд, когда TODO нет открытого левого меню закрыто или focused-выдачи
    acc ::= searchAh

    // index всегда доступен для приёма управляющих сигналов.
    acc ::= indexAh

    if ( indexWelcomeRW().nonEmpty )
      acc ::= new WelcomeAh( indexWelcomeRW )

    if ( searchRW.value.isMapInitialized )
      acc ::= mapCommonAh

    // Контроллер плитки -- тоже где-то в начале.
    acc ::= gridAdsAh

    // Базовые экшены всей выдачи перехватываем всегда и в самую первую очередь.
    // Сюда приходят оптовые или частые сообщения от геолокации, маячков, листенеров размеров экрана.
    acc ::= indexStateAh

    // Собрать все контроллеры в пачку.
    composeHandlers( acc: _* )
  }


  // Отработать инициализацию роутера в самом начале конструктора.
  {
    val jsRouterFut = SrvRouter.ensureJsRouter()
    jsRouterFut.andThen { case tryRes =>
      dispatch( JsRouterStatus(tryRes) )
    }

    val jsRouterReadyP = Promise[None.type]()
    val unSubscribeJsRouterF = subscribe( jsRouterRW ) { jsRouterPotProxy =>
      if (jsRouterPotProxy.value.nonEmpty) {
        // Запустить инициализацию начального индекса выдачи.
        try {
          if (indexRW.value.resp.isEmpty) {
            dispatch( GetIndex( None ) )
          }
          // Запустить получения гео-маркеров с сервера.
          if (searchMapRcvrsPotRW.value.isEmpty) {
            dispatch( RcvrMarkersInit )
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
