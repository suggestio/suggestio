package io.suggest.log.remote

import io.suggest.log.{ILogAppender, MLogMsg, MLogReport}
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model._
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.16 17:40
  * Description: Поддержка логгирования на сервер в формате RemoteError.
  */
final class RemoteLogAppender( httpConfig: () => HttpClientConfig )
  extends ILogAppender
{

  override def logAppend(logMsgs: Seq[MLogMsg]): Unit = {
    val logReport = MLogReport(
      msgs = logMsgs.toList,
    )

    // Организовать запрос на сервер по указанной ссылке.
    val req = HttpReq.routed(
      // Отсутствие роуты не отрабатываем, т.к. это совсем трэш-ситуация, которая должна быть отработана
      // где-то на уровне тестов или где-то ещё.
      route = routes.controllers.RemoteLogs.receive(),
      data = HttpReqData(
        headers = Map(
          HttpConst.Headers.CONTENT_TYPE -> MimeConst.APPLICATION_JSON,
        ),
        body = Json
          .toJson( logReport )
          .toString(),
        config = httpConfig(),
      ),
    )

    val fut = HttpClient.execute( req )
      .respAuthFut
      .successIfStatus( HttpConst.Status.NO_CONTENT )

    // Залоггировать проблемы реквеста в консоль.
    for (ex <- fut.failed) {
      //val n = "\n"
      var msg = ErrorMsgs.RME_LOGGER_REQ_FAIL

      if (scalajs.LinkingInfo.developmentMode)
        msg = msg + "\n " + logMsgs.mkString("\n ") + "\n" + ex /*+ " " + ex.getStackTrace.mkString(n,n,n)*/

      println( msg )
    }
  }

}
