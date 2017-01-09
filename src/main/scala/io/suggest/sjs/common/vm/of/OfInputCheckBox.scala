package io.suggest.sjs.common.vm.of

import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.01.16 15:23
 * Description: Поддержка чекбоксов для [[OfInput]].
 */
trait OfInputCheckBox extends OfInput {

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      val el1 = el.asInstanceOf[HTMLInputElement]
      el1.`type`.equalsIgnoreCase("checkbox")
    }
  }

}
