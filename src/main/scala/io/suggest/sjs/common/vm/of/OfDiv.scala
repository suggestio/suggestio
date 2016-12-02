package io.suggest.sjs.common.vm.of

import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.01.16 17:21
 * Description: Реализация [[Of]] для DIV.
 */
trait OfDiv extends Of {

  override type Dom_t = HTMLDivElement

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    // TODO Opt Можно ведь тут выставить == вместо equalsIgnoreCase()?
    el.tagName.equalsIgnoreCase("DIV")
  }

}
