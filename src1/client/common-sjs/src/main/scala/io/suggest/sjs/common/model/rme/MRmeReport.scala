package io.suggest.sjs.common.model.rme

import io.suggest.err.ErrorConstants.Remote._
import io.suggest.msg.ErrorMsg_t
import io.suggest.sjs.common.log.Severity
import org.scalajs.dom

import scala.scalajs.js

/** Модель отчёта об ошибки для MRemoteError. */

object MRmeReport {

  /** Сериализация инстанса модели [[MRmeReport]] в JSON-представление. */
  def toJson(report: MRmeReport): js.Dictionary[js.Any] = {
    val d = js.Dictionary.empty[js.Any]

    d(SEVERITY_FN) = report.severity
    d(URL_FN) = report.url
    d(MSG_FN) = report.msg
    for (s <- report.state)
      d(STATE_FN) = s
    for (errCode <- report.errCode)
      d(ERROR_CODE_FN) = errCode

    d
  }

}


/** Класс модели-контейнера данных отчёта об ошибке в почти произвольной форме. */
case class MRmeReport(
  msg       : String,
  severity  : Severity,
  url       : String              = dom.window.location.href,
  errCode   : Option[ErrorMsg_t]  = None,
  state     : Option[String]      = None
)
