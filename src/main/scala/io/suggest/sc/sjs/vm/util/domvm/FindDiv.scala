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

  def applyOpt(elOpt: Option[Dom_t]): Option[T] = {
    elOpt map apply
  }
}


trait IFindEl extends TypeT {

  def find(): Option[T]

}


trait FindApplyUtil extends IApplyEl with GetElById {
  override type Dom_t <: Element

  protected def _find(id: String): Option[T] = {
    applyOpt {
      getElementById[Dom_t](id)
    }
  }
}


/** Трейт для поиска элементов по статическому id. */
trait FindElT
  extends IFindEl
  with DomId
  with IApplyEl
  with GetElById
  with FindApplyUtil
{
  override def find(): Option[T] = _find(DOM_ID)
}


/** Частоиспользуемый код для vm-компаньонов, обслуживающих div'ы со статическими id. */
trait FindDiv extends FindElT with GetDivById {
  override type Dom_t = HTMLDivElement
}


/** Поиск элемента по динамическому id. */
trait FindElIndexedIdT extends IndexedDomId with IApplyEl with GetElById with FindApplyUtil {
  def find(arg: DomIdArg_t): Option[T] = {
    _find( getDomId(arg) )
  }
}
