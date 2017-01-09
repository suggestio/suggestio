package io.suggest.sjs.common.vm.of

import org.scalajs.dom.{Node, EventTarget}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 10:52
 * Description: Трейт "Of Event Target" для приведения инстансов EventTarget к VM'кам.
 */

trait OfEventTarget extends OfBase {

  /** Безопасное приведение указанного EventTarget к данной VM'ке. */
  def ofEventTarget(et: EventTarget): Option[T]

  /** Безопаное приведение указанного EventTarget или какой-то родительской ноды к данной VM'ке. */
  def ofEventTargetUp(et: EventTarget): Option[T]

}


/** Связывание [[OfNodeHtmlEl]].ofNode() и [[OfEventTarget]]. */
trait OfEventTargetNode extends OfEventTarget with OfNodeHtmlEl {

  override def ofEventTarget(et: EventTarget): Option[T] = {
    _ofEventTargetNodeWith(et)(ofNodeUnsafe)
  }

  override def ofEventTargetUp(et: EventTarget): Option[T] = {
    _ofEventTargetNodeWith(et)(ofNodeUp)
  }

  protected def _ofEventTargetNodeWith(et: EventTarget)(f: Node => Option[T]): Option[T] = {
    if (OfUtil.isInstance(et)  &&  et.asInstanceOf[MaybeNode].nodeType.isDefined) {
      f( et.asInstanceOf[Node] )
    } else {
      None
    }
  }

}


/** Безопасный интерфейс к собственным полям Node. */
@js.native
sealed trait MaybeNode extends js.Object {
  def nodeType: UndefOr[_]
}

