package io.suggest.sjs.common.vm

import io.suggest.primo.{IUnderlying, TypeT}
import io.suggest.sjs.common.vm.attr.AttrVmT
import io.suggest.sjs.common.vm.css.CssClassT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import org.scalajs.dom.Node

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 10:31
 * Description: Базовая комбинация из нескольких vm-аддонов в одном флаконе.
 */

object Vm {

  /**
   * Генератор анонимной фунции-экстрактора underlying-элемента.
   * @tparam Tmin Минимальный возвращаемый тип underlying-элемента.
   */
  def underlyingF[Tmin <: js.Object] = {
    { el: IVm { type T <: Tmin} =>
      el._underlying
    }
  }

}


/** Базовый интерфейс для всех VM'ок, т.е. view-model'ей. */
trait IVm extends TypeT with IUnderlying {

  /** Тип backend-узла (-элемента).*/
  override type T <: js.Object

  /** Backend-элемент, для которого реализуется высокоуровневый доступ в этой vm'ке. */
  override def _underlying: T

}


/** Базовый набор для тегов разных. */
trait VmT
  extends CssClassT
  with AttrVmT
  with SelfRemoveT


/** Дефолтовая реализация [[VmT]]. */
case class Vm[T1 <: Node](
  override val _underlying: T1
)
  extends VmT
{
  override type T = T1
}
