package io.suggest.sjs.common.vm.input

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLInputElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 22:15
 * Description: Поддержка взаимодействия с полем checked.
 */
trait Checked extends IVm {

  override type T <: HTMLInputElement

  def isChecked: Boolean = _underlying.checked

  def invertChecked: Boolean = {
    val v = !isChecked
    setChecked(v)
    v
  }

  def setChecked(isChecked: Boolean): Unit = {
    _underlying.checked = isChecked
    _underlying.value   = isChecked.toString
  }

}
