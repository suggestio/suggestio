package io.suggest.sc.log

import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.vm.wnd.WindowVm
import org.scalajs.dom.ErrorEvent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.07.16 19:56
  * Description: Поддержка логгирования глобальных ошибок.
  */
object GlobalErrorHandler extends Log {

  def start(): Unit = {
    // TODO Надо бы capture вместо bubble, см. https://developer.mozilla.org/en-US/docs/Web/API/GlobalEventHandlers/onerror
    WindowVm().addEventListener("error") { e: ErrorEvent =>
      val msg = e.filename + " (" + e.lineno + "," + e.colno + ") " + e.message
      LOG.error(msg = msg)
    }
  }

}