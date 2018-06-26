package io.suggest.sjs.common.model.rme

import io.suggest.err.ErrorConstants
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.MimeConst
import io.suggest.proto.HttpConst
import io.suggest.sjs.common.log.{ILogAppender, LogMsg, Severity}
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.{HttpStatuses, Xhr}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.text.StringUtil

import scala.concurrent.Future
import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 17:40
  * Description: Поддержка логгирования на сервер в формате RemoteError.
  */
abstract class RmeLogAppender extends ILogAppender {

  // TODO Нужна защита от StackOverflow, чтобы избежать вызова логгера во время инициализации object'ов.
  //private var _isReady: Boolean = false

  /** Куда делать реквест. Функция, возвращающая route. */
  def route: Route

  def minSeverity: Severity //= Severities.Warn

  private def _logAppendInto(logMsg: LogMsg, route: Route): Future[_] = {
    // Организовать запрос на сервер по указанной ссылке.
    val fut = Xhr.successIfStatus( HttpStatuses.NO_CONTENT ) {
      Xhr.send(
        route   = route,      // TODO Отработать отсутствие роуты через /sc/error
        headers = {
          val hdrCt = HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_JSON
          hdrCt :: Nil
        },
        body    = {
          val c = ErrorConstants.Remote
          val report = MRmeReport(
            severity = logMsg.severity,
            // TODO Рендерить в report необходиые поля, а не собирать строковой message. Чтобы на сервере индексировалось всё.
            msg     = StringUtil.strLimitLen( logMsg.onlyMainText, c.MSG_LEN_MAX ),
            state   = logMsg.fsmState.map( StringUtil.strLimitLen(_, c.STATE_LEN_MAX) ),
            errCode = logMsg.code
          )
          val json = MRmeReport.toJson(report)
          JSON.stringify(json)
        }
      )
    }

    // Залоггировать проблемы реквеста в консоль.
    for (ex <- fut.failed) {
      val n = "\n"
      println( ErrorMsgs.RME_LOGGER_REQ_FAIL + " " + logMsg + " " + ex + " " + ex.getStackTrace.mkString(n,n,n))
    }

    fut
  }

  override def logAppend(logMsg: LogMsg): Unit = {
    if (logMsg.severity >= minSeverity) {
      try {
        _logAppendInto(logMsg, route)
      } catch {
        case ex: Throwable =>
          // Бывает, что роута недоступна (js-роутер ещё не готов). Надо молча подавлять такие ошибки.
          println(ex.getMessage)
      }
    }
  }

}
