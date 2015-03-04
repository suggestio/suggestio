package io.suggest.xadv.ext.js.runner.c

import org.scalajs.dom

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
    try {
      val popup = dom.window.open(
        url       = routes.controllers.Static.popupCheckContent().url,
        target    = "sio-popup-test",
        features  = "toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=yes, resizable=yes, copyhistory=yes, width=400, height=300"
      )
      popup.focus()
      popup.close()
      true

    } catch {
      case ex: Throwable =>
        dom.console.error("Popup windows are unavailable: %s: %s", ex.getClass.getSimpleName, ex.getMessage)
        false
    }
  }

}
