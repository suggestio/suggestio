package io.suggest.sjs.common.view.safe.wnd.cs

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom
import org.scalajs.dom.{Window, Element}
import org.scalajs.dom.raw.CSSStyleDeclaration

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:38
 * Description: Аккуратный доступ к window.getComputedStyle().
 */
trait SafeGetComputedStyleT extends ISafe {

  override type T <: Window

  def getComputedStyle(elt: Element): Option[CSSStyleDeclaration] = {
    SafeGetComputedStyleStub(_underlying)
      .getComputedStyle
      .toOption
      .map { _ =>
        _underlying.getComputedStyle(elt)
      }
  }

}


/** Интерфейс для window для аккуратного доступа к getComputedStyle(). */
sealed trait SafeGetComputedStyleStub extends js.Object {
  val getComputedStyle: UndefOr[_] = js.native
}

object SafeGetComputedStyleStub {
  def apply(wnd: Window = dom.window): SafeGetComputedStyleStub = {
    wnd.asInstanceOf[SafeGetComputedStyleStub]
  }
}
