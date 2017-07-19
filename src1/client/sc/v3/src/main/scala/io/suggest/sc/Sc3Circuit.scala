package io.suggest.sc

import diode.react.ReactConnector
import io.suggest.dev.JsScreenUtil
import io.suggest.sc.inx.c.IndexAh
import io.suggest.sc.inx.m.{GetIndex, MScIndex, MScIndexState}
import io.suggest.sc.root.m.MScRoot
import io.suggest.sc.router.c.JsRouterInitAh
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.{ErrorMsg_t, ErrorMsgs}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 12:00
  * Description: Main circuit новой выдачи. Отрабатывает весь интерфейс выдачи v3.
  */
object Sc3Circuit extends CircuitLog[MScRoot] with ReactConnector[MScRoot] {

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

  // TODO actionHandler нужно будет пересобирать на-лету TODO исходя из флагов состояния и TODO кэшировать для ускорения.
  override protected val actionHandler = {
    val api = new Sc3ApiXhrImpl

    // Листенер инициализации роутера. TODO Выкидывать его после окончания инициализации.
    val jsRouterInitAh = new JsRouterInitAh(
      modelRW = zoomRW(_.jsRouter)(_.withJsRouter(_))
    )

    val indexRW = zoomRW(_.index)(_.withIndex(_))
    val rootRO = zoom(m => m)

    // Подготовить контроллер index'а выдачи.
    val indexAh = new IndexAh(
      api = api,
      modelRW = indexRW.zoomRW(_.resp)(_.withResp(_)),
      stateRO = rootRO
    )

    // Собрать все контроллеры в пачку.
    composeHandlers(
      indexAh,
      jsRouterInitAh
    )
  }


  // constructor
  Future {
    dispatch( GetIndex( None ) )
  }

}
