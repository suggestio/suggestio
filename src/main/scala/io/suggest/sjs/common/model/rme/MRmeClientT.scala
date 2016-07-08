package io.suggest.sjs.common.model.rme

import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.{HttpStatuses, Xhr}

import scala.concurrent.Future
import scala.scalajs.js.JSON

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.07.16 17:52
  * Description: Модель для отправки данных об ошибках на сервера suggest.io.
  * MRme == MRemoteError.
  */
trait MRmeClientT {

  /** Роута с ресурсом и методом отправки отчёта. */
  def route: Route

  /** Отправить один об ошибке на сервер. */
  def sendReport(report: MRmeReport): Future[_] = {
    val r = route
    Xhr.successIfStatus( HttpStatuses.NO_CONTENT ) {
      Xhr.send(
        method  = r.method,
        url     = r.url,
        headers = Seq(
          Xhr.HDR_CONTENT_TYPE -> Xhr.MIME_JSON
        ),
        body    = {
          val json = MRmeReport.toJson(report)
          val str = JSON.stringify(json)
          Some(str)
        }
      )
    }
  }

  /** Это тоже для отправки, просто удобнее вызывать из SjsLogger. */
  def submitSjsLoggerMsg(msg: String, ex: Throwable = null, state: Option[String] = None): Future[_] = {
    val msg1 = if (ex == null) {
      msg
    } else {
      val n = "\n"
      msg + " " + ex.getClass.getSimpleName + " " + ex.getMessage + ex.getStackTrace.mkString(n,n,n)
    }
    val report = MRmeReport(
      msg   = msg1,
      state = state
    )
    sendReport(report)
  }

}
