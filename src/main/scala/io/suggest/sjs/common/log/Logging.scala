package io.suggest.sjs.common.log

import io.suggest.sjs.common.msg.ErrorMsg_t

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
      l.logAppend(logMsg)
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
  * @param fsmState Состояние FSM, если есть.
  */
case class LogMsg(
  severity  : Severity,
  from      : String,
  code      : Option[ErrorMsg_t]  = None,
  message   : Option[String]      = None,
  exception : Option[Throwable]   = None,
  fsmState  : Option[String]      = None
) {

  override def toString: ErrorMsg_t = {
    // Нааккумулировать данных для логгирования из модели logMsg в строку.
    var tokensAcc = List.empty[String]

    val d = " "

    for (ex <- exception) {
      val n = "\n"
      tokensAcc = d :: ex.getClass.getSimpleName :: d :: ex.getMessage :: n :: ex.getStackTrace.mkString(n,n,n) :: tokensAcc
    }
    for (msg <- message)
      tokensAcc = d :: msg :: tokensAcc
    for (code <- code)
      tokensAcc = d :: code :: tokensAcc
    for (fsmState <- fsmState)
      tokensAcc = d :: "[" :: fsmState.toString :: "]" :: tokensAcc
    tokensAcc = from :: ":" :: d :: tokensAcc

    // Отрендерить в логи итог работы...
    tokensAcc.mkString
  }

}

/** Интерфейс потребителя сообщений логгирования. */
trait ILogAppender {
  def logAppend(logMsg: LogMsg): Unit
}
