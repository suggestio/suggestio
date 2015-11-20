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

  def value_=(v: VT): Unit = {
    _underlying.value = v.toString
  }

}


trait DoubleInputValueT extends InputValueT[Double] {
  override protected def _parseInputValue(v: String): Double = {
    v.toDouble
  }
}


trait IntInputValueT extends InputValueT[Int] {
  override protected def _parseInputValue(v: String): Int = {
    v.toInt
  }
}
