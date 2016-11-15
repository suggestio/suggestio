package io.suggest.sjs.common.model.rme

import io.suggest.sjs.common.log.{ILogAppender, LogMsg}
import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.{HttpStatuses, Xhr}

import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 17:40
  * Description: Поддержка логгирования на сервер в формате RemoteError.
  */
abstract class RmeLogAppender extends ILogAppender {

  /** Куда делать реквест. Функция, возвращающая route. */
  def route: Route

  def minSeverity: Int = 0

  private def _logAppendInto(logMsg: LogMsg, route: Route): Unit = {
    // Организовать запрос на сервер по указанной ссылке.
    Xhr.successIfStatus( HttpStatuses.NO_CONTENT ) {
      Xhr.send(
        method  = route.method,
        url     = route.url,
        headers = Seq(
          Xhr.HDR_CONTENT_TYPE -> Xhr.MIME_JSON
        ),
        body    = {
          val report = MRmeReport(
            severity = logMsg.severity,
            // TODO Рендерить в report необходиые поля, а не собирать строковой message. Чтобы на сервере индексировалось всё.
            msg     = logMsg.toString,
            state   = logMsg.fsmState,
            errCode = logMsg.code
          )
          val json = MRmeReport.toJson(report)
          val str = JSON.stringify(json)
          Some(str)
        }
      )
    }
  }

  override def logAppend(logMsg: LogMsg): Unit = {
    if (logMsg.severity >= minSeverity) {
      try {
        _logAppendInto(logMsg, route)
      } catch {
        case ex: Throwable =>
          // Бывает, что роута недоступна (js-роутер ещё не готов). Надо молча подавлять такие ошибки.
          //println(ex.getMessage)
      }
    }
  }

}
