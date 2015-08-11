package io.suggest.lk.ad.form.init

import io.suggest.lk.ad.form.model.MColorPalette
import io.suggest.lk.router.jsRoutes
import io.suggest.sjs.common.controller.IInit
import io.suggest.ad.form.AdFormConstants._
import io.suggest.sjs.common.util.ISjsLogger
import io.suggest.sjs.common.view.CommonPage
import org.scalajs.dom
import org.scalajs.dom.MessageEvent
import org.scalajs.dom.raw.{HTMLDivElement, WebSocket}
import org.scalajs.jquery.jQuery

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:16
 * Description: Инициализация websocket для связи с сервером по простому каналу.
 */
trait AdFormWsInit extends IInit with ISjsLogger {

  /** Запуск инициализации текущего модуля. */
  abstract override def init(): Unit = {
    super.init()
    try {
      _initWs()
    } catch {
      case ex: Throwable =>
        error("E6754", ex)
    }
  }


  /** Инициализация websocket. */
  private def _initWs(): Unit = {
    // Аккуратно извлечь wsId для генерации ws-ссылки. Если есть.
    val jq = jQuery("#" + WS_ID_INPUT_ID)
    Option( jq )
      .flatMap { jqSel => Option( jqSel.`val`() ) }
      .filter { !js.isUndefined(_) }
      .map { _.asInstanceOf[String].trim }
      .filter { !_.isEmpty }
      .foreach { wsId =>
        _initWsForId(wsId)
        jq.remove()
      }
  }

  /** Непосредственная инициализация ws. */
  private def _initWsForId(wsId: String): Unit = {
    val route = jsRoutes.controllers.MarketAd.ws(wsId)
    val wsUrl = route.webSocketURL()
      .replace("ws:", "wss:")
    log("wsUrl = " + wsUrl)
    val ws = new WebSocket(wsUrl)
    // Закрывать ws при закрытии вкладки.
    CommonPage.wsCloseOnPageClose(ws)

    // Обрабатывать сообщения от сервера.
    ws.onmessage = { msg: MessageEvent =>
      val payload = JSON.parse( msg.data.asInstanceOf[String] )
      MColorPalette.maybeFromJson(payload)
        .foreach { _applyColorPalette }
    }
  }

  /** Применить полученную палитру к текущей веб-странице. */
  private def _applyColorPalette(pal: MColorPalette): Unit = {
    val palDivJq = jQuery("#" + COLORS_DIV_ID)
    palDivJq
      .children()
      .remove()

    // Выставить основной цвет фон.
    if (pal.colors.nonEmpty) {
      val d = dom.document

      // Создаём контейнер, куда будут закидываться создаваемые теги.
      val container = d.createElement("div")
        .asInstanceOf[HTMLDivElement]

      // Отрендерить палитру.
      pal.colors
        .iterator
        .zipWithIndex
        .foreach { case (color, i) =>
          val el = d.createElement("div")
            .asInstanceOf[HTMLDivElement]
          el.setAttribute("class",      "color-block " + CSS_JS_PALETTE_COLOR)
          el.setAttribute("data-color", color)
          el.setAttribute("style",      "background-color: #" + color + ";")
          container.appendChild(el)
        }
      palDivJq.append(container)
    }
  }

}
