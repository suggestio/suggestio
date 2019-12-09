package io.suggest.sjs.common.vm.attr

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 11:02
  * Description: Аддоны для быстрого чтения-записи input.value с парсингом.
  */
trait InputValueT[VT] extends IVm {

  override type T <: HTMLInputElement

  def value: Option[VT] = {
    val v = _underlying.value
    if (v.isEmpty)
      None
    else
      Some( _parseInputValue(v) )
  }

  protected def _parseInputValue(v: String): VT

}


trait StringInputValueT extends InputValueT[String] {
  override protected def _parseInputValue(v: String): String = {
    v
  }
}

