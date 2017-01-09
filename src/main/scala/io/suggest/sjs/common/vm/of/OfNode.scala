package io.suggest.sjs.common.vm.of

import org.scalajs.dom.Node
import org.scalajs.dom.raw.Element

import scala.annotation.tailrec

/**
  * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 10:42
 * Description: Трейт абстрактной поддержки приведения ноды к текущей VM.
 */
trait OfNode extends OfBase {

  /**
   * Попытаться привести ноду к текущей VM'ке.
   * @param n Предполагаемая нода.
   * @return Some(Vm) если нода подошла.
   *         Иначе None.
   */
  def ofNode(n: Node): Option[T] = {
    if (OfUtil.isInstance(n))
      ofNodeUnsafe(n)
    else
      None
  }

  def ofNodeUnsafe(n: Node): Option[T]

  /**
   * Попытаться привести текущую или любую родительскую ноду к текущей VM'ке.
   * @param n Предполагаемая нода.
   * @return Some(Vm), если нода или родительская нода подходит под Vm.
   *         None в остальных случаях.
   */
  @tailrec
  final def ofNodeUp(n: Node): Option[T] = {
    if ( OfUtil.isInstance(n) ) {
      val res0 = ofNodeUnsafe(n)
      if (res0.isDefined) {
        res0
      } else {
        ofNodeUp( n.parentNode )
      }
    } else {
      None
    }
  }

}


/** Связывание [[OfElementHtmlEl]].ofEl() и [[OfNode]]. */
trait OfNodeHtmlEl extends OfElementHtmlEl with OfNode {

  override def ofNodeUnsafe(n: Node): Option[T] = {
    if (n.nodeType == Node.ELEMENT_NODE) {
      ofElUnsafe( n.asInstanceOf[Element] )
    } else {
      None
    }
  }

}
