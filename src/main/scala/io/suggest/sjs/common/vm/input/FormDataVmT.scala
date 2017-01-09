package io.suggest.sjs.common.vm.input

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.FormData
import org.scalajs.dom.raw.HTMLFormElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.01.16 10:56
 * Description: Поддержка получения инстанса FormData для текущей формы.
 */
trait FormDataVmT extends IVm {

  override type T <: HTMLFormElement

  /** @return Инстанс FormData для текущей формы. */
  def formData: FormData = {
    new FormData(_underlying)
  }

}
