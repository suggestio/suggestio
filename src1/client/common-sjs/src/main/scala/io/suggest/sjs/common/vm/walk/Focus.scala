package io.suggest.sjs.common.vm.walk

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.16 21:58
  * Description: Поддержка захвата и сброса фокусировки у элемента.
  */
trait Focus extends IVm {

  override type T <: HTMLElement

  def focus(): Unit = {
    _underlying.focus()
  }

  def blur(): Unit = {
    _underlying.blur()
  }

}
