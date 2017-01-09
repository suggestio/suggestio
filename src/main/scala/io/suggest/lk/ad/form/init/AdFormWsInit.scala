package io.suggest.lk.ad.form.init

import io.suggest.lk.ad.form.model.MColorPalette
import io.suggest.lk.router.jsRoutes
import io.suggest.sjs.common.controller.IInit
import io.suggest.ad.form.AdFormConstants._
import io.suggest.sjs.common.log.ILog
import io.suggest.sjs.common.msg.ErrorMsgs
import io.suggest.sjs.common.view.{CommonPage, VUtil}
import org.scalajs.dom.MessageEvent
import org.scalajs.dom.raw.WebSocket
import org.scalajs.jquery.jQuery

import scala.scalajs.js
import scala.scalajs.js.JSON

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:16
 * Description: Инициализация websocket для связи с сервером по простому каналу.
 */
trait AdFormWsInit extends IInit with ILog {

  /** Запуск инициализации текущего модуля. */
  abstract override def init(): Unit = {
    super.init()
    try {
      _initWs()
    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.AD_FORM_WS_INIT_FAILED, ex )
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
    val wsUrl = route.webSocketURL( CommonPage.isSecure )
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
      // Создаём контейнер, куда будут закидываться создаваемые теги.
      val container = VUtil.newDiv()

      // Отрендерить палитру.
      pal.colors
        .iterator
        .zipWithIndex
        .foreach { case (color, i) =>
          val el = VUtil.newDiv()
          el.setAttribute("class",      "color-block " + CSS_JS_PALETTE_COLOR)
          el.setAttribute("data-color", color)
          el.setAttribute("style",      "background-color: #" + color + ";")
          container.appendChild(el)
        }
      palDivJq.append(container)
    }
  }

}
