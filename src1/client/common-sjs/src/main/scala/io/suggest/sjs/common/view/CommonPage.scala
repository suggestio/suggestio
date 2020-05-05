package io.suggest.sjs.common.view

import io.suggest.event.DomEvents
import io.suggest.sjs.common.vm.evtg.EventTargetVm
import io.suggest.sjs.common.vm.evtg.EventTargetVm.RichEventTarget
import org.scalajs.dom
import japgolly.univeq._
import org.scalajs.dom.BeforeUnloadEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 10:10
 * Description: Поддержка типичных действий на страницы вынесена в этот view.
 */
object CommonPage {

  /** Запустить этот код, когда страница будет готова. */
  def onReady(f: () => _): Unit = {
    // Пример без jQuery, покрывающий большинство ситуаций, взят отсюда - https://stackoverflow.com/a/7053197
    if (dom.document.readyState !=* "loading") {
      f()
    } else if (EventTargetVm.isIe()) {
      dom.document.addEventListener4s( DomEvents.READY_STATE_CHANGE ) { _ =>
        if (dom.document.readyState ==* "complete")
          f()
      }
    } else {
      dom.document.addEventListener4s( DomEvents.DOM_CONTENT_LOADED )(_ => f())
    }
  }


  /** Запустить переданный код при закрытии/обновлении страницы. */
  def onClose(f: () => _): Unit = {
    dom.window.addEventListener4s[BeforeUnloadEvent]( DomEvents.BEFORE_UNLOAD )(_ => f())
  }

}
