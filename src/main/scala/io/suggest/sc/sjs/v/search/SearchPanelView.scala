package io.suggest.sc.sjs.v.search

import io.suggest.sc.sjs.c.SearchPanelCtl
import io.suggest.sjs.common.view.safe.evtg.SafeEventTarget
import org.scalajs.dom
import io.suggest.sc.ScConstants.Search._
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:23
 * Description: Вьюшка для панели поиска.
 */
object SearchPanelView {

  def initFtsField(): Unit = {
    val input = dom.document.getElementById( FTS_FIELD_ID )
    val fieldSafe = SafeEventTarget(input)
    // Навешиваем события
    fieldSafe.addEventListener("keyup") { (e: Event) =>
      SearchPanelCtl.onFtsFieldKeyUp(e)
    }
    fieldSafe.addEventListener("focus") { (e: Event) =>
      SearchPanelCtl.onFtsFieldFocus(e)
    }
    fieldSafe.addEventListener("blur") { (e: Event) =>
      SearchPanelCtl.onFtsFieldBlur(e)
    }
  }

}
