package io.suggest.sc.sjs.vm.util.domvm

import io.suggest.primo.TypeT
import io.suggest.sc.sjs.vm.util.domvm.get.GetDivById
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 14:59
 * Description: Метод поиска в DOM элемента и заворачивания в ViewModel.
 */
trait FindDiv extends GetDivById with DomId with TypeT {

  def apply(div: HTMLDivElement): T

  def find(): Option[T] = {
    getDivById(DOM_ID)
      .map { apply }
  }

}
