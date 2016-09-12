package io.suggest.sjs.common.vm.of

import org.scalajs.dom.raw.{HTMLElement, HTMLLinkElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.09.16 15:06
  * Description: Поддержка of'инга link-тегов.
  */
trait OfLink extends Of {

  override type Dom_t <: HTMLLinkElement

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    el.tagName.equalsIgnoreCase("LINK")
  }

}
