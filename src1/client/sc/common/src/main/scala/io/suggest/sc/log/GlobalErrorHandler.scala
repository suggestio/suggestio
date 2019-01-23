package io.suggest.sc.log

import io.suggest.sjs.common.log.Log
import org.scalajs.dom.ErrorEvent
import io.suggest.sjs.common.vm.evtg.EventTargetVm._
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.07.16 19:56
  * Description: Поддержка логгирования глобальных ошибок.
  */
object GlobalErrorHandler extends Log {

  def start(): Unit = {
    // TODO Надо бы capture вместо bubble, см. https://developer.mozilla.org/en-US/docs/Web/API/GlobalEventHandlers/onerror
    dom.window.addEventListener4s("error") { e: ErrorEvent =>
      val msg = e.filename + " (" + e.lineno + "," + e.colno + ") " + e.message
      LOG.error(msg = msg)
    }
  }

}
