package io.suggest.lk.price.m

import io.suggest.adv.AdvConstants.Price.Json.{PRICE_HTML_FN, PERIOD_REPORT_HTML_FN}

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.01.16 12:32
 * Description: Модель положительного ответа сервера на XHR-запрос цены и прочего.
 */
object Resp {

  /** Десериализация из JSON. */
  def fromJson(resp: Any): Resp = {
    val json0 = resp match {
      case s: String =>
        JSON.parse(resp.asInstanceOf[String])
      case json =>
        json
    }
    val json1 = json0.asInstanceOf[js.Dictionary[String]]

    apply(
      priceHtml  = json1(PRICE_HTML_FN),
      periodHtml = json1(PERIOD_REPORT_HTML_FN)
    )
  }

}

case class Resp(
  priceHtml   : String,
  periodHtml  : String
)
