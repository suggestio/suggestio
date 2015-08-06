package io.suggest.sc.sjs.vm.util.domvm

import io.suggest.primo.TypeT
import io.suggest.sc.sjs.vm.util.domvm.get.{GetElById, GetDivById}
import org.scalajs.dom.{Element, Node}
import org.scalajs.dom.raw.HTMLDivElement

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


trait FindElT
  extends IFindEl
  with DomId
  with IApplyEl
  with GetElById
{

  override type Dom_t <: Element

  override def find(): Option[T] = {
    getElementById[Dom_t](DOM_ID)
      .map { apply }
  }

}


/** Частоиспользуемый код для vm-компаньонов, обслуживающих div'ы. */
trait FindDiv extends FindElT with GetDivById {
  override type Dom_t = HTMLDivElement
}
