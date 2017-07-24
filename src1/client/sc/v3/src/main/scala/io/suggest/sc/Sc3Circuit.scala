package io.suggest.sc

import diode.react.ReactConnector
import io.suggest.common.event.WndEvents
import io.suggest.dev.JsScreenUtil
import io.suggest.maps.m.MMapS
import io.suggest.sc.init.MSc3Init
import io.suggest.sc.inx.c.{IndexAh, IndexStateAh, WelcomeAh}
import io.suggest.sc.inx.m.{GetIndex, MScIndex, MScIndexState}
import io.suggest.sc.m.ScreenReset
import io.suggest.sc.root.m.MScRoot
import io.suggest.sc.router.c.JsRouterInitAh
import io.suggest.sc.search.c.SearchAh
import io.suggest.sc.search.m.MScSearch
import io.suggest.sc.styl.{ScCss, ScCssFactoryModule}
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.spa.OptFastEq.Wrapped
import io.suggest.sjs.common.spa.StateInp
import io.suggest.sjs.common.vm.wnd.WindowVm
import org.scalajs.dom.Event
import play.api.libs.json.Json

import scala.concurrent.Future

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
          screen = JsScreenUtil.getScreen
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

  val rootRO = zoom(m => m)


  // Кэш action-handler'ов
  private val searchAh = new SearchAh( modelRW = searchRW )

  private val indexAh = new IndexAh(
    api     = api,
    modelRW = indexRW,
    stateRO = rootRO
  )

  private val indexStateAh = new IndexStateAh(
    modelRW = indexStateRW
  )


  override protected def actionHandler = {
    var acc = List.empty[HandlerFunction]

    // Листенер инициализации роутера. Выкидывать его после окончания инициализации.
    if ( !jsRouterRW().isReady ) {
      acc ::= new JsRouterInitAh(
        modelRW = jsRouterRW
      )
    }

    // top-level search AH всегда ожидает команд, когда TODO нет открытого левого меню закрыто или focused-выдачи
    acc ::= searchAh

    // index всегда доступен для приёма управляющих сигналов.
    acc ::= indexAh

    if ( indexWelcomeRW().nonEmpty )
      acc ::= new WelcomeAh( indexWelcomeRW )

    // Базовые экшены всей выдачи перехватываем всегда и в самую первую очередь.
    // Сюда приходят оптовые или частые сообщения от геолокации, маячков, листенеров размеров экрана.
    acc ::= indexStateAh

    // Собрать все контроллеры в пачку.
    composeHandlers( acc: _* )
  }


  // constructor
  {
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

    // Заставить систему получить index с сервера. TODO Ожидать геолокации, маячков с помощью таймера.
    Future {
      dispatch( GetIndex( None ) )
    }
  }


  /** Зуммер для получения инстанса динамических аргументов рендера ScCss. */
  private val scCssArgsRO = indexRW.zoom(_.scCssArgs)

  private var _scCssCacheOpt: Option[ScCss] = None

  // Подписка на события реальных изменений, связанных со стилями ScCss:
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
