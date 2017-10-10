package io.suggest.sjs.common.view

import io.suggest.common.event.DomEvents
import io.suggest.proto.HttpConst
import org.scalajs.dom
import org.scalajs.dom.WebSocket
import org.scalajs.jquery._

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 10:10
 * Description: Поддержка типичных действий на страницы вынесена в этот view.
 */
object CommonPage {

  /** Запустить этот код, когда страница будет готова. */
  def onReady(f: () => _): Unit = {
    jQuery(dom.document)
      .ready(f: js.Function0[_])
  }


  /** Запустить переданный код при закрытии/обновлении страницы. */
  def onClose(f: () => _): Unit = {
    jQuery(dom.window)
      .on(DomEvents.BEFORE_UNLOAD, f: js.Function0[_])
  }


  def wsCloseOnPageClose(ws: WebSocket): Unit = {
    onClose { () =>
      ws.close()
    }
  }

  /** Используется ли зашифрованное соединение для текущей страницы?
    * @see [[http://stackoverflow.com/q/414809]]
    */
  def isSecure: Boolean = {
    dom.document.location.protocol == HttpConst.Proto.HTTPS_
  }

}
