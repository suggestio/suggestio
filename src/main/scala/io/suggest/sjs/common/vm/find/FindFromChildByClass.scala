package io.suggest.sjs.common.vm.find

import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.{Vm, VmT}
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 14:45
 * Description: Трейт для быстрого поиска текущей модели для узла и его родителей.
 */
trait FindFromChildByClass extends IApplyEl {

  /** css-класс по которому определяется искомый элемент. */
  protected def _findParentCssMarker: String

  /**
   * Попытаться найти элемент текущей модели от текущего узла вверх по дереву.
   * @param node Текущий узел.
   * @return Some() если элемент модели найден текущей модели найден.
   */
  def findUpToNode(node: VmT): Option[T] = {
    for (safeEl <- VUtil.hasCssClass(node, _findParentCssMarker)) yield {
      val el = safeEl._underlying.asInstanceOf[Dom_t]
      apply(el)
    }
  }
  def findUpToNode(node: Node): Option[T] = {
    findUpToNode( Vm(node) )
  }

}
