package io.suggest.sc.sjs.util.logs

import io.suggest.sc.sjs.vm.SafeWnd
import org.scalajs.dom.ErrorEvent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.07.16 19:56
  * Description: Поддержка логгирования глобальных ошибок.
  */
object GlobalErrorHandler extends ScSjsLogger {

  def start(): Unit = {
    // TODO Надо бы capture вместо bubble, см. https://developer.mozilla.org/en-US/docs/Web/API/GlobalEventHandlers/onerror
    SafeWnd.addEventListener("error") { e: ErrorEvent =>
      val msg = e.filename + " (" + e.lineno + "," + e.colno + ") " + e.message
      error(msg)
    }
  }

}
