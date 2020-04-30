package io.suggest.log

import io.suggest.common.html.HtmlConstants
import io.suggest.msg.{ErrorMsg_t, ErrorMsgs}
import japgolly.univeq.UnivEq
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

  /** Обработка входящих [[LogMsg]] системами логгирования. */
  def handleLogMsg(logMsg: LogMsg): Unit = {
    for (l <- LOGGERS) {
      try {
        l.logAppend( logMsg :: Nil )
      } catch { case ex: Throwable =>
        println( ErrorMsgs.LOG_APPENDER_FAIL + " " + l + " " + ex.getClass.getName + " " + ex.getMessage)
      }
    }
  }

  def handleLogMsgSafe(logMsg: LogMsg): Unit = {
    try {
      Logging.handleLogMsg(logMsg)
    } catch { case ex: Throwable =>
      // Подавлять ошибки внутри самих логгеров.
      println( ErrorMsgs.ALL_LOGGERS_FAILED + HtmlConstants.SPACE + ex.getMessage + HtmlConstants.SPACE + logMsg )
    }
  }

  def getUrlOpt(): Option[String] =
    Try( Option(dom.window.location.href) ).toOption.flatten

}



object LogMsg {
  @inline implicit def univEq: UnivEq[LogMsg] = UnivEq.force


  /** Чтобы не было warning - structural types - need import, тут определён отдельный класс.
    * Без кучи warning'ов компилятора можно было заинлайнить это в builder (без конструктора класса). */
  final case class builder[R](classSimpleName: String, andThen: LogMsg => R, url: Option[String] = Logging.getUrlOpt())
                             (severity: Severity) {
    /** Из-за наличия default-аргументов, эта функция объявленна в классе, не в функции. */
    def apply(errorMsg: ErrorMsg_t = null,  ex: Throwable = null,  msg: Any = null ): R = {
      val lm = LogMsg(
        severity  = severity,
        from      = classSimpleName,
        code      = Option(errorMsg),
        message   = Option(msg).map(_.toString),
        exception = Option(ex),
        url       = url,
      )
      andThen( lm )
    }

  }

}
/** Класс с данными для логгирования. Он отправляется в каждый конкретный логгер.
  *
  * @param severity Важность.
  * @param from Откуда сообщение (класс).
  * @param code Код ошибки по списку кодов.
  * @param message Произвольное сообщение об ошибке.
  * @param exception Исключение, если есть.
  */
case class LogMsg(
  severity  : Severity,
  from      : String,
  code      : Option[ErrorMsg_t]  = None,
  message   : Option[String]      = None,
  exception : Option[Throwable]   = None,
  url       : Option[String]      = None,
) {

  /** Рендер в строку, но без severity, fsmState, code. */
  def onlyMainText: String = {
    // Нааккумулировать данных для логгирования из модели logMsg в строку.
    var tokensAcc = List.empty[String]

    val d = HtmlConstants.SPACE
    val n = HtmlConstants.NEWLINE_UNIX.toString

    for (ex <- exception)
      tokensAcc = d :: ex.getClass.getSimpleName :: d :: ex.getMessage :: n :: ex.getStackTrace.mkString(n,n,n) :: tokensAcc

    for (msg <- message)
      tokensAcc = n :: msg :: tokensAcc

    tokensAcc = from :: ":" :: d :: tokensAcc

    // Отрендерить в логи итог работы...
    tokensAcc.mkString
  }

}


/** Интерфейс потребителя сообщений логгирования. */
trait ILogAppender {

  def logAppend(logMsgs: Seq[LogMsg]): Unit

  override def toString = getClass.getSimpleName

}
