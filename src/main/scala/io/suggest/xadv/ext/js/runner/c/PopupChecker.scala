package io.suggest.xadv.ext.js.runner.c

import org.scalajs.dom
import org.scalajs.dom.raw.Window

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.15 19:07
 * Description: Перед началом работы необходимо убедится, что браузер не заблокирует все всплывающие окна.
 */
object PopupChecker {

  /** Синхронная проверка на способность браузера не блокировать окна. */
  def isPopupAvailable(): Boolean = {
    // Запуск проверки того, открылось ли окно на деле. http://stackoverflow.com/a/27725432
    var popup: Window = null
    try {
      popup = dom.window.open(
        url       = routes.controllers.Static.popupCheckContent().url,
        target    = "sio-popup-test",
        features  = "toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=400, height=300"
      )
      popup.focus()
      popup.close()
      true

    } catch {
      case ex: Throwable =>
        dom.window.addEventListener("message", sioPopupOpened(_: dom.MessageEvent), false)
        // Надо попробовать задействовать https://developer.mozilla.org/en-US/docs/Web/API/Window/postMessage
        dom.console.error("Popup window are unavailable: %s: %s", ex.getClass.getSimpleName, ex.getMessage)
        false
    }
  }


  /**
   * Юзер разблокировал блокировщик попапов.
   * @param evt Событие сообщения.
   */
  protected def sioPopupOpened(evt: dom.MessageEvent): Unit = {
    dom.console.log("DOM msg received: [", evt.data, "] from", evt.source)
    if (evt.data == ("reloaded" : js.Any)) {
      // TODO Нужно реагировать на разблокировку попапов: рестартовать текущий процесс БЕЗ перезагрузки страницы.
      dom.document.location.reload()
    }
  }

}
