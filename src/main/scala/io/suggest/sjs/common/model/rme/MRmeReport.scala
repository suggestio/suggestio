package io.suggest.sjs.common.model.rme

import io.suggest.err.ErrorConstants.Remote._
import org.scalajs.dom

import scala.scalajs.js

/** Модель отчёта об ошибки для MRemoteError. */

object MRmeReport {

  /** Сериализация инстанса модели [[MRmeReport]] в JSON-представление. */
  def toJson(report: MRmeReport): js.Dictionary[String] = {
    val d = js.Dictionary.empty[String]

    d(URL_FN) = report.url
    d(MSG_FN) = report.msg
    for (s <- report.state)
      d(STATE_FN) = s

    d
  }

}

/** Класс модели-контейнера данных отчёта об ошибке в почти произвольной форме. */
case class MRmeReport(
  msg     : String,
  url     : String          = dom.window.location.href,
  state   : Option[String]  = None
)
