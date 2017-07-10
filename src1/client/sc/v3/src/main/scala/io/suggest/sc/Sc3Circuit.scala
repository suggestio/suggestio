package io.suggest.sc

import diode.react.ReactConnector
import io.suggest.sc.root.m.MScRoot
import io.suggest.sjs.common.log.CircuitLog
import io.suggest.sjs.common.msg.{ErrorMsg_t, ErrorMsgs}

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
    MScRoot()
  }

  override protected def actionHandler = {
    // TODO
    ???
  }

}
