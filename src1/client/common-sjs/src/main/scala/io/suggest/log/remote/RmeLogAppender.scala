package io.suggest.log.remote

import io.suggest.err.ErrorConstants
import io.suggest.log.{ILogAppender, LogMsg, Severity}
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.routes.PlayRoute
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
  def route: PlayRoute

  def minSeverity: Severity //= Severities.Warn

  private def _logAppendInto(logMsgs: Seq[LogMsg], route: PlayRoute): Future[_] = {
    // Организовать запрос на сервер по указанной ссылке.
    // TODO XXX Отправлять всю пачку
    val logMsg = logMsgs.head

    val req = HttpReq.routed(
      // TODO Отработать отсутствие роуты через /sc/error
      route = route,
      data = HttpReqData(
        headers = Map(
          HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_JSON
        ),
        body = {
          val c = ErrorConstants.Remote
          val report = MRmeReport(
            severity = logMsg.severity,
            // TODO Рендерить в report необходиые поля, а не собирать строковой message. Чтобы на сервере индексировалось всё.
            msg     = StringUtil.strLimitLen( logMsg.onlyMainText, c.MSG_LEN_MAX ),
            errCode = logMsg.code,
          )
          val json = MRmeReport.toJson( report )
          JSON.stringify(json)
        }
      )
    )

    val fut = HttpClient.execute( req )
      .respAuthFut
      .successIfStatus( HttpConst.Status.NO_CONTENT )

    // Залоггировать проблемы реквеста в консоль.
    for (ex <- fut.failed) {
      val n = "\n"
      println( ErrorMsgs.RME_LOGGER_REQ_FAIL + " " + logMsg + " " + ex + " " + ex.getStackTrace.mkString(n,n,n))
    }

    fut
  }

  override def logAppend(logMsgs: Seq[LogMsg]): Unit = {
    val logMsgs2 = logMsgs.filter( _.severity >= minSeverity )
    try {
      _logAppendInto( logMsgs2, route )
    } catch {
      case ex: Throwable =>
        // Бывает, что роута недоступна (js-роутер ещё не готов). Надо молча подавлять такие ошибки.
        println(ex.getMessage)
    }
  }

}
