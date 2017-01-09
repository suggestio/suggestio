package io.suggest.sjs.common.vm.of

import org.scalajs.dom.raw.{HTMLElement, Element}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.01.16 11:21
 * Description: Поддержка приведения абстрактного Element к другим вещам.
 */
trait OfElement extends OfBase {

  def ofEl(el: Element): Option[T] = {
    if (OfUtil.isInstance(el)) {
      ofElUnsafe(el)
    } else {
      None
    }
  }

  def ofElUnsafe(el: Element): Option[T]

}


/** Связывание [[OfHtmlElement]].ofHtmlEl() и просто Element. */
trait OfElementHtmlEl extends OfHtmlElement with OfElement {

  override def ofElUnsafe(el: Element): Option[T] = {
    val el1 = el.asInstanceOf[MaybeHtmlElement]
    if (el1.style.isDefined) {
      ofHtmlElUnsafe( el.asInstanceOf[HTMLElement] )
    } else {
      None
    }
  }

}


/** Безопасный интерфейс к собственным полям HTMLElement. */
@js.native
sealed trait MaybeHtmlElement extends js.Object {
  var style: UndefOr[_]
}
