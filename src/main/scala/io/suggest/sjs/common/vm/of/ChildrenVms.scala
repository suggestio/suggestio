package io.suggest.sjs.common.vm.of

import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.raw.ParentNode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.16 17:25
 * Description: Дедубликация внутренней логики обхода node children с of-извлечением.
 */
trait ChildrenVms extends IVm {

  /** Тип экземпляра дочерней VM. */
  type ChildVm_t

  override type T <: ParentNode

  /** Статическая сторона child-модели. */
  protected def _childVmStatic: OfElement { type T = ChildVm_t }

  /** Сборка итератора дочерних vm'ок на основе дочерних элементов. */
  protected def _childrenVms: Iterator[ChildVm_t] = {
    val vm = _childVmStatic
    DomListIterator( _underlying.children )
      .flatMap { vm.ofEl }
  }

}
