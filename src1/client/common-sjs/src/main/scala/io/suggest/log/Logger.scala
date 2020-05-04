package io.suggest.log

import scala.annotation.elidable
import scala.annotation.elidable._
import scala.util.Try

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

  implicit final class LogClsApi[T]( private val logHolder: T ) extends AnyVal {

    @elidable( SEVERE )
    def error = _logMsgBuilder( LogSeverities.Error )

    @elidable( WARNING )
    def warn = _logMsgBuilder( LogSeverities.Warn )

    @elidable( INFO )
    def info = _logMsgBuilder( LogSeverities.Info )

    @elidable( FINE )
    def log = _logMsgBuilder( LogSeverities.Log )


    private def _logMsgBuilder = {
      val c = logHolder.getClass
      val _className = Try( c.getSimpleName )
        .orElse( Try( c.getName ) )
        .getOrElse( c.toString )
      Logging.logMsgBuilder( _className, Logging.handleLogMsgSafe )
    }

  }

}
