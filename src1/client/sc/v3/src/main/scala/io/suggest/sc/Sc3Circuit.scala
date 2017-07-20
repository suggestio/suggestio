package io.suggest.sc

import diode.react.ReactConnector
import io.suggest.dev.JsScreenUtil
import io.suggest.sc.inx.c.{IndexAh, WelcomeAh}
import io.suggest.sc.inx.m.{GetIndex, MScIndex, MScIndexState}
import io.suggest.sc.root.m.MScRoot
import io.suggest.sc.router.c.JsRouterInitAh
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.spa.OptFastEq.Wrapped

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:00
  * Description: Main circuit новой выдачи. Отрабатывает весь интерфейс выдачи v3.
  */
object Sc3Circuit extends CircuitLog[MScRoot] with ReactConnector[MScRoot] {

  import MScIndex.MScIndexFastEq
  import io.suggest.sc.inx.m.MWelcomeState.MWelcomeStateFastEq


  override protected def CIRCUIT_ERROR_CODE: ErrorMsg_t = ErrorMsgs.SC_FSM_EVENT_FAILED

  override protected def initialModel = {
    // TODO Десериализовать состояние из URL или откуда-нибудь ещё.
    MScRoot(
      index = MScIndex(
        state = MScIndexState(
          screen = JsScreenUtil.getScreen
        )
      )
    )
  }

  val api = new Sc3ApiXhrImpl

  private val jsRouterRW = zoomRW(_.jsRouter)(_.withJsRouter(_))

  private val indexRW = zoomRW(_.index)(_.withIndex(_))
  private val indexWelcomeRW = indexRW.zoomRW(_.welcome)(_.withWelcome(_))

  val rootRO = zoom(m => m)

  // TODO actionHandler нужно будет пересобирать на-лету TODO исходя из флагов состояния и TODO кэшировать для ускорения.
  override protected def actionHandler = {
    var acc = List.empty[HandlerFunction]

    // Листенер инициализации роутера. Выкидывать его после окончания инициализации.
    if ( !jsRouterRW().isReady ) {
      acc ::= new JsRouterInitAh(
        modelRW = jsRouterRW
      )
    }

    // index всегда доступен для приёма управляющих сигналов.
    acc ::= new IndexAh(
      api     = api,
      modelRW = indexRW,
      stateRO = rootRO
    )

    if ( indexWelcomeRW().nonEmpty )
      acc ::= new WelcomeAh( indexWelcomeRW )


    // Собрать все контроллеры в пачку.
    composeHandlers( acc: _* )
  }


  // constructor
  Future {
    dispatch( GetIndex( None ) )
  }

}
