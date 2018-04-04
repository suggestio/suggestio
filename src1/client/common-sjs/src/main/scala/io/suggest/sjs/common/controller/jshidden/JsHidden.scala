package io.suggest.sjs.common.controller.jshidden

import io.suggest.err.ErrorConstants
import org.scalajs.jquery.{JQuery, jQuery}
import io.suggest.js.hidden.JsHiddenConstants._


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 11:09
 * Description: Реализация подсистемы сокрытия-отображения элементов js-hidden.
 */

object JsHidden {

  def CSS_SELECTOR = "." + CSS_CLASS

  def processAllIn(jq: JQuery): Unit = {
    val res = jq.find(CSS_SELECTOR)
    processFound(res)
  }

  def processFound(res: JQuery): Unit = {
    if (res.length > 0) {
      val errors = res.find("." + ErrorConstants.FORM_CSS_CLASS)
      if (errors.length > 0) {
        res.show()
      } else {
        res.hide()
      }
    }
  }

  def processAll(): Unit = {
    processFound( jQuery(CSS_SELECTOR) )
  }

}

