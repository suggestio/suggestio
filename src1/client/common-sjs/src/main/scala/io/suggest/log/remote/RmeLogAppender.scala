package io.suggest.log.remote

import io.suggest.err.ErrorConstants
import io.suggest.log.{ILogAppender, LogSeverities, LogSeverity, MLogMsg, MLogReport}
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 17:40
  * Description: Поддержка логгирования на сервер в формате RemoteError.
  */
class RmeLogAppender(
                      minSeverity: LogSeverity = LogSeverities.Warn
                    )
  extends ILogAppender
{

  private def _logAppendInto( logMsgs: Seq[MLogMsg] ): Future[_] = {
    // Организовать запрос на сервер по указанной ссылке.
    // TODO XXX Отправлять всю пачку
    val req = HttpReq.routed(
      // TODO Отработать отсутствие роуты через /sc/error
      route = routes.controllers.RemoteLogs.receive(),
      data = HttpReqData(
        headers = Map(
          HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_JSON
        ),
        body = {
          val logRep = MLogReport(
            msgs = logMsgs.toList,
          )
          Json.toJson( logRep ).toString()
        }
      )
    )

    val fut = HttpClient.execute( req )
      .respAuthFut
      .successIfStatus( HttpConst.Status.NO_CONTENT )

    // Залоггировать проблемы реквеста в консоль.
    for (ex <- fut.failed) {
      val n = "\n"
      println( ErrorMsgs.RME_LOGGER_REQ_FAIL + "\n " + logMsgs.mkString("\n ") + "\n" + ex + " " + ex.getStackTrace.mkString(n,n,n) )
    }

    fut
  }

  override def logAppend(logMsgs: Seq[MLogMsg]): Unit = {
    val logMsgs2 = logMsgs.filter( _.severity.value >= minSeverity.value )
    if (logMsgs2.nonEmpty) {
      try {
        _logAppendInto( logMsgs2 )
      } catch {
        case ex: Throwable =>
          // Бывает, что роута недоступна (js-роутер ещё не готов). Надо молча подавлять такие ошибки.
          println(getClass.getSimpleName, ex.getMessage)
      }
    }
  }

}
