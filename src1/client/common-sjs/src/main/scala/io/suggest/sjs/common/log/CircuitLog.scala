package io.suggest.sjs.common.log

import diode.Circuit
import io.suggest.sjs.common.msg.ErrorMsg_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 19:15
  * Description: Поддержка sio-логгера в цепях diode.
  * Для использования этого логгинга нужно экстендить этот CircuitLog вместо стандартного diode.Circuit.
  */
trait CircuitLog[M <: AnyRef] extends Circuit[M] with Log {

  protected def CIRCUIT_ERROR_CODE: ErrorMsg_t

  override def handleFatal(action: Any, e: Throwable): Unit = {
    LOG.error( CIRCUIT_ERROR_CODE, e, action )
    super.handleFatal(action, e)
  }

  override def handleError(msg: String): Unit = {
    LOG.warn( CIRCUIT_ERROR_CODE, msg = msg )
    super.handleError(msg)
  }

}
