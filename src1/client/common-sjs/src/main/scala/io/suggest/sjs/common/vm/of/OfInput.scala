package io.suggest.sjs.common.vm.of

import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.01.16 14:42
 * Description: Поддержка [[Of]].of() для input-элементов.
 */
trait OfInput extends Of {

  override type Dom_t <: HTMLInputElement

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    el.tagName.equalsIgnoreCase("INPUT")
  }

}
