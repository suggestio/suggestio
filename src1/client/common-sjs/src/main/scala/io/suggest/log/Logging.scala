package io.suggest.log

import io.suggest.common.html.HtmlConstants
import io.suggest.msg.ErrorMsgs
import io.suggest.spa.DAction
import org.scalajs.dom

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 16:42
  * Description: Базовые куски системы логгирования.
  */


/** Статический компонент логгинга: приём сообщений и отправка их на обработку. */
object Logging {

  /** Динамически-настраиваемый список логгеров, вызываемых в прямом порядке. */
  var LOGGERS: List[ILogAppender] = {
    new JsConsoleAppender :: Nil
  }

  /** Обработка входящих [[MLogMsg]] системами логгирования. */
  def handleLogMsg(logMsg: MLogMsg): Unit = {
    for (l <- LOGGERS) {
      try {
        l.logAppend( logMsg :: Nil )
      } catch { case ex: Throwable =>
        println( ErrorMsgs.LOG_APPENDER_FAIL + " " + l + " " + ex.getClass.getName + " " + ex.getMessage)
      }
    }
  }

  def handleLogMsgSafe(logMsg: MLogMsg): Unit = {
    try {
      Logging.handleLogMsg(logMsg)
    } catch { case ex: Throwable =>
      // Подавлять ошибки внутри самих логгеров.
      println( ErrorMsgs.ALL_LOGGERS_FAILED + HtmlConstants.SPACE + ex.getMessage + HtmlConstants.SPACE + logMsg )
    }
  }

  def getUrlOpt(): Option[String] = {
    Try( Option(dom.window.location.href) )
      .toOption
      .flatten
  }


  def logMsgBuilder = {
    MLogMsg.builder(
      url             = getUrlOpt(),
      stackTraceLen   = if (scalajs.LinkingInfo.developmentMode) 6 else 1,
    )
  }

}


/** Интерфейс потребителя сообщений логгирования. */
trait ILogAppender {

  def logAppend(logMsgs: Seq[MLogMsg]): Unit

  override def toString = getClass.getSimpleName

}


/** Экшены для сложных circuit-логгеров. */
trait ILogAction extends DAction
