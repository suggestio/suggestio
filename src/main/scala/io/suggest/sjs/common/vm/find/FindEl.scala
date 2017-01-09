package io.suggest.sjs.common.vm.find

import io.suggest.primo.{IApplyOpt1, TypeT}
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.util._
import org.scalajs.dom.raw.HTMLDivElement
import org.scalajs.dom.{Element, Node}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 14:59
 * Description: Аддоны для сборки static vm с поиском в DOM необходимого backend-элемента.
 */
trait TypeDomT {
  type Dom_t <: Node
}


/** Интерфейс для apply() у всех vm'ок.
  * Методы apply и applyOpt были вынесены в более абстрактный IApply1. */
trait IApplyEl extends IApplyOpt1 with TypeDomT {

  override final type ApplyArg_t = Dom_t

}


trait IFindEl extends TypeT {

  def find(): Option[T]

}


trait FindApplyUtil extends IApplyEl {
  override type Dom_t <: Element

  protected def _find(id: String): Option[T] = {
    val elOpt = VUtil.getElementById[Dom_t](id)
    applyOpt( elOpt )
  }
}


/** Трейт для поиска элементов по статическому id. */
trait FindElT
  extends IFindEl
  with DomId
  with IApplyEl
  with FindApplyUtil
{
  override def find(): Option[T] = _find(DOM_ID)
}


/** Частоиспользуемый код для vm-компаньонов, обслуживающих div'ы со статическими id. */
trait FindDiv extends FindElT {
  override type Dom_t = HTMLDivElement
}


/** Трейт API быстрого и простого поиска элемента по динамическому id. */
trait FindElDynIdT extends DynDomId with IApplyEl with FindApplyUtil {
  def find(arg: DomIdArg_t): Option[T] = {
    _find( getDomId(arg) )
  }
}
