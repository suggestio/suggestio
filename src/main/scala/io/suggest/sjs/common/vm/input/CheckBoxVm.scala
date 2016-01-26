package io.suggest.sjs.common.vm.input

import io.suggest.sjs.common.vm.of.OfInputCheckBox
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 22:04
 * Description: Скрытый чекбокс.
 */
trait CheckBoxVmStaticT extends OfInputCheckBox {

  override type Dom_t = HTMLInputElement
}


object CheckBoxVm extends CheckBoxVmStaticT {
  override type T = CheckBoxVm
}

import CheckBoxVm.Dom_t


trait CheckBoxVmT extends Checked {
  override type T = Dom_t
}

case class CheckBoxVm(override val _underlying: Dom_t)
  extends CheckBoxVmT
