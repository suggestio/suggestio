package io.suggest.sjs.common.view.safe.overflow

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 17:02
 * Description: Аддон для выставления overflow = hidden.
 */
trait OvfHiddenT extends ISafe {

  override type T <: HTMLElement

  def overflowHidden(): Unit = {
    _underlying.style.overflow = "hidden"
  }

}
