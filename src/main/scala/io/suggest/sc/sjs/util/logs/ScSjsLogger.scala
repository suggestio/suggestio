package io.suggest.sc.sjs.util.logs

import io.suggest.fsm.StateData
import io.suggest.sc.sjs.util.router.srv.routes
import io.suggest.sjs.common.fsm.SjsFsm
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.model.rme.MRmeClientT
import io.suggest.sjs.common.util.SjsLogger

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.05.16 9:29
  * Description: Трейт логгирования, используемый в sc-sjs.
  *
  * Вынесен в отдельный трейт для унифицированного управления логгированием в выдаче.
  * Появился в ожиданиях скорого внедрения server-side логгирования через MRemoteError.
  */
object ScSjsLogger extends MRmeClientT {

  override def route: Route = routes.controllers.Sc.handleScError()

}


/** Трейт логгера с поддержкой MRemoteError. */
trait ScSjsLogger extends SjsLogger {

  /** Дополнительно можно логгировать какое-то "состояние". */
  protected def _logState: Option[String] = None

  override def error(msg: String): Unit = {
    super.error(msg)
    ScSjsLogger.submitSjsLoggerMsg(msg, state = _logState)
  }

  override def error(msg: String, ex: Throwable): Unit = {
    super.error(msg, ex)
    ScSjsLogger.submitSjsLoggerMsg(msg, ex, _logState)
  }

  override def warn(msg: String): Unit = {
    super.warn(msg)
    ScSjsLogger.submitSjsLoggerMsg(msg, state = _logState)
  }

  override def warn(msg: String, ex: Throwable): Unit = {
    super.warn(msg, ex)
    ScSjsLogger.submitSjsLoggerMsg(msg, ex, _logState)
  }

}


/** Трейт логгера с поддержкой MRemoteError для FSM. */
trait ScSjsFsmLogger extends ScSjsLogger with SjsFsm with StateData {

  /** Дополнительно можно логгировать какое-то "состояние". */
  override protected def _logState: Option[String] = {
    val stateMsg = _state.toString + " " + _stateData
    Some(stateMsg)
  }

}
