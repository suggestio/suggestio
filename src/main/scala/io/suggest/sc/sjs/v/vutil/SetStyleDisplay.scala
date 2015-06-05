package io.suggest.sc.sjs.v.vutil

import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.06.15 18:42
 * Description: Краткое выставление style.display
 */
trait SetStyleDisplay {

  protected def setDisplay(el: HTMLElement, v: String): Unit = {
    el.style.display = v
  }
  
  protected def displayBlock(el: HTMLElement): Unit = {
    setDisplay(el, "block")
  }
  
  protected def displayNone(el: HTMLElement): Unit = {
    setDisplay(el, "none")
  }

}
