package io.suggest.sc.sjs.vm.util

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.domvm.IApplyEl
import io.suggest.sjs.common.view.safe.{SafeEl, SafeElT}
import org.scalajs.dom.Node

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.08.15 14:45
 * Description: Трейт для быстрого поиска элемента текущей модели для узла и его родителей.
 */
trait FindVmForCss extends IApplyEl {

  /** css-класс по которому определяется искомый элемент. */
  protected def _findParentCssMarker: String

  /**
   * Попытаться найти элемент текущей модели от текущего узла вверх по дереву.
   * @param node Текущий узел.
   * @return Some() если элемент модели найден текущей модели найден.
   */
  def findUpToNode(node: SafeElT): Option[T] = {
    VUtil.hasCssClass(node, _findParentCssMarker)
      .map { safeEl =>
      val el = safeEl._underlying.asInstanceOf[Dom_t]
      apply(el)
    }
  }
  def findUpToNode(node: Node): Option[T] = {
    findUpToNode( SafeEl(node) )
  }

}
