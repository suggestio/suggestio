package io.suggest.sjs.common.view.safe.display

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 11:27
 * Description: Быстрый доступ к _underlying.style.display и значениям.
 */
trait SetDisplayEl extends ISafe {
  
  override type T <: HTMLElement
  
  protected def setDisplay(newDisplay: String): Unit = {
    _underlying.style.display = newDisplay
  }

  /** Скрыть из отображения текущий DOM-элемент. */
  protected def displayNone() = setDisplay("none")

  /** Отобразить текущий DOM-элемент блочно. */
  protected def displayBlock() = setDisplay("block")

  protected def isHidden: Boolean = {
    val d = _underlying.style.display
    d != null && !d.isEmpty && d != "none"
  }
  
}
