package io.suggest.sjs.common.view

import org.scalajs.dom
import org.scalajs.dom.{WebSocket, Document}
import org.scalajs.dom.raw.{HTMLBodyElement, HTMLHeadElement, HTMLElement}
import org.scalajs.jquery._

import scala.scalajs.js
import scala.scalajs.js.UndefOr

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
      .on("beforeunload", f: js.Function0[_])
  }


  def wsCloseOnPageClose(ws: WebSocket): Unit = {
    onClose { () =>
      ws.close()
    }
  }

}
