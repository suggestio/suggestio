package io.suggest.sc.sjs.vm.util.domvm

import io.suggest.primo.TypeT
import io.suggest.sc.sjs.vm.util.domvm.get.{GetSpanById, GetDivById}
import org.scalajs.dom.Node
import org.scalajs.dom.raw.{HTMLSpanElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 14:59
 * Description: Метод поиска в DOM элемента и заворачивания в ViewModel.
 */
trait IApplyEl extends TypeT {
  type Dom_t <: Node

  def apply(div: Dom_t): T
}


trait IFindEl extends TypeT {

  def find(): Option[T]
}

trait FindDiv extends IFindEl with GetDivById with DomId with IApplyEl {

  override type Dom_t = HTMLDivElement

  def find(): Option[T] = {
    getDivById(DOM_ID)
      .map { apply }
  }

}


trait FindSpan extends IFindEl with GetSpanById with DomId with IApplyEl {

  override type Dom_t = HTMLSpanElement

  override def find(): Option[T] = {
    getSpanById(DOM_ID)
      .map { apply }
  }
}