package io.suggest.sjs.common.vm.input

import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.IApplyEl
import org.scalajs.dom.{Node, Element}
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 22:04
 * Description: Скрытый чекбокс.
 */
trait CheckBoxVmStaticT extends IApplyEl {

  override type Dom_t = HTMLInputElement

  def of(vm: VmT): Option[T] = {
    val resOpt = VUtil.hasOrHasParent(vm) { _vm =>
      val el = _vm._underlying.asInstanceOf[Element]
      el.tagName.equalsIgnoreCase("input") && el.asInstanceOf[Dom_t].`type`.equalsIgnoreCase("checkbox")
    }
    for (res <- resOpt) yield {
      val el1 = res._underlying.asInstanceOf[Dom_t]
      apply(el1)
    }
  }

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
