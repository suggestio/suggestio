package io.suggest.log

import scala.annotation.elidable
import scala.annotation.elidable._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 18:11
  * Description: Поддержка логгирования второго поколения в scala.js.
  */


/** Реализация простого логгирования в каком-то классе. */
trait Log {
  @inline final def logger: Log = this
}


object Log {

  implicit final class LogClsApi[T]( private val cls: T ) extends AnyVal {

    @elidable( SEVERE )
    def error = _logMsgBuilder( Severities.Error )

    @elidable( WARNING )
    def warn = _logMsgBuilder( Severities.Warn )

    @elidable( INFO )
    def info = _logMsgBuilder( Severities.Info )

    @elidable( FINE )
    def log = _logMsgBuilder( Severities.Log )

    private def _classSimpleName: String = {
      val c = cls.getClass
      try {
        c.getSimpleName
      } catch { case _: Throwable =>
        c.getName
      }
    }

    private def _logMsgBuilder =
      LogMsg.builder( _classSimpleName, Logging.handleLogMsgSafe )(_)

  }

}
