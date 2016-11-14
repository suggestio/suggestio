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
  * @param call Куда делать реквест.
  */
class RmeLogAppender(call: Route) extends ILogAppender {

  override def logAppend(logMsg: LogMsg): Unit = {
    // Организовать запрос на сервер по указанной ссылке.
    Xhr.successIfStatus( HttpStatuses.NO_CONTENT ) {
      Xhr.send(
        method  = call.method,
        url     = call.url,
        headers = Seq(
          Xhr.HDR_CONTENT_TYPE -> Xhr.MIME_JSON
        ),
        body    = {
          val report = MRmeReport(
            // TODO Рендерить в report необходиые поля, а не собирать строковой message. Чтобы на сервере индексировалось всё.
            msg   = logMsg.toString,
            state = logMsg.fsmState
          )
          val json = MRmeReport.toJson(report)
          val str = JSON.stringify(json)
          Some(str)
        }
      )
    }
  }

}
