package io.suggest.sjs.common.log

import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}

import scala.annotation.elidable
import scala.annotation.elidable._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 18:11
  * Description: Поддержка логгирования второго поколения в scala.js.
  */

/** Трейт для классов-логгеров,  */
trait LoggerT {

  /** Результат getClass.getSimpleName. */
  def classSimpleName: String

  /** Состояние FSM, если есть. */
  def fsmState: Option[String]


  @elidable( SEVERE )
  def error(errorMsg: ErrorMsg_t = null, ex: Throwable = null, msg: Any = null): Unit = {
    doLog(Severities.Error, errorMsg, msg, ex)
  }
  @elidable( WARNING )
  def warn(errorMsg: ErrorMsg_t  = null, ex: Throwable = null, msg: Any = null): Unit = {
    doLog(Severities.Warn, errorMsg, msg, ex)
  }
  @elidable( INFO )
  def info(errorMsg: ErrorMsg_t  = null, ex: Throwable = null, msg: Any = null): Unit = {
    doLog(Severities.Info, errorMsg, msg, ex)
  }
  @elidable( FINE )
  def log(errorMsg: ErrorMsg_t   = null, ex: Throwable = null, msg: Any = null): Unit = {
    doLog(Severities.Log, errorMsg, msg, ex)
  }


  def doLog(severity: Severity, errorMsg: ErrorMsg_t, msg: Any, ex: Throwable): Unit = {
    val logMsg = LogMsg(
      severity  = severity,
      from      = classSimpleName,
      code      = Option(errorMsg),
      message   = Option(msg).map(_.toString),
      exception = Option(ex),
      fsmState  = fsmState
    )
    doLog(logMsg)
  }
  def doLog(logMsg: LogMsg): Unit = {
    try {
      Logging.handleLogMsg(logMsg)
    } catch { case ex: Throwable =>
      // Подавлять ошибки внутри самих логгеров.
      println( ErrorMsgs.ALL_LOGGERS_FAILED + " " + ex.getMessage )
    }
  }

}


/** Самый простой логгер без интеграции с FSM. */
class Logger(override val classSimpleName: String) extends LoggerT {
  override def fsmState: Option[String] = None
}


/** Интерфейс логгирования в каком-то классе. */
trait ILog {
  def LOG: Logger
}
/** Реализация простого логгирования в каком-то классе. */
trait Log extends ILog {
  override def LOG: Logger = {
    val csn = try {
      getClass.getSimpleName
    } catch { case _: Throwable =>
      getClass.getName
    }
    new Logger( csn )
  }
}
