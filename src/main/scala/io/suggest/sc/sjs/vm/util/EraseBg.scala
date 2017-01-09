package io.suggest.sc.sjs.vm.util

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 16:42
 * Description: Аддон для стирания фона у HTML-элемента.
 */
trait EraseBg extends IVm {

  override type T <: HTMLElement

  protected def ERASED_BG_COLOR = "#ffffff"

  def eraseBg(): Unit = {
    _underlying.style.backgroundColor = ERASED_BG_COLOR
  }

}
