package io.suggest.sjs.common.vm.walk

import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.IApplyEl
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.08.15 14:50
 * Description: Аддон классов vm'ок для пошагового перемещения в рамках уровня по однородным элементам.
 */
trait PrevNextSiblingsVmT extends IVm {

  override type T <: Node
  /** Внутри IApplyEl {...} T конфликтует с this.T, поэтому нужно другое имя для типа T. */
  private type T1 = T

  /** Тип текущего элемента. */
  type Self_t <: PrevNextSiblingsVmT    // TODO this.type не работает, какая-то ошибка возникает.

  protected def _companion: IApplyEl { type Dom_t = T1; type T = Self_t }

  /** Хелпер для дедубликации кода методов previous() и next(). */
  protected def __prevNextHelper(f: T => Node): Option[Self_t] = {
    val prevElOrNull = f(_underlying).asInstanceOf[T]
    Option(prevElOrNull)
      .map { el => _companion(el) }
  }

  /** Предыдущий по DOM элемент того же типа и уровня. */
  def previous: Option[Self_t] = {
    __prevNextHelper(_.previousSibling)
  }

  /** Следующий по DOM элемент того же типа и уровня. */
  def next: Option[Self_t] = {
    __prevNextHelper(_.nextSibling)
  }

}


/** Утиль для доступа к двоюродным элементам. */
trait PrevNextSiblingCousinUtilT extends PrevNextSiblingsVmT {

  type Parent_t <: PrevNextSiblingsVmT { type T <: Node }

  protected def _parent: Option[Parent_t]

  protected def __prevNextCousinHelper(fragF: Parent_t => Option[Parent_t])
                                      (blockF: Parent_t => Option[Self_t]): Option[Self_t] = {
    for {
      frag      <- _parent if frag._underlying.firstChild == _underlying
      prevFrag  <- fragF(frag)
      prevBlock <- blockF(prevFrag)
    } yield {
      prevBlock
    }
  }

}
