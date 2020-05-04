package io.suggest.log.remote

import io.suggest.err.ErrorConstants.Remote._
import io.suggest.log.LogSeverity
import io.suggest.msg.ErrorMsg_t
import org.scalajs.dom

import scala.scalajs.js

/** Модель отчёта об ошибки для MRemoteError. */
@deprecated
object MRmeReport {

  /** Сериализация инстанса модели [[MRmeReport]] в JSON-представление. */
  def toJson(report: MRmeReport): js.Dictionary[js.Any] = {
    val d = js.Dictionary.empty[js.Any]

    d(SEVERITY_FN) = report.severity.value
    d(URL_FN) = report.url
    d(MSG_FN) = report.msg
    for (errCode <- report.errCode)
      d(ERROR_CODE_FN) = errCode

    d
  }

}


/** Класс модели-контейнера данных отчёта об ошибке в почти произвольной форме. */
@deprecated
case class MRmeReport(
                       msg       : String,
                       severity  : LogSeverity,
                       url       : String              = dom.window.location.href,
                       errCode   : Option[ErrorMsg_t]  = None,
                     )
