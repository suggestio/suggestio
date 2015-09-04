package io.suggest.sjs.common.vm.wnd.compstyle

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom
import org.scalajs.dom.raw.CSSStyleDeclaration
import org.scalajs.dom.{Element, Window}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 16:38
 * Description: Аккуратный доступ к window.getComputedStyle().
 */
trait GetComputedStyleT extends IVm {

  override type T <: Window

  def getComputedStyle(elt: Element): Option[CSSStyleDeclaration] = {
    WndGetComputedStyleStub(_underlying)
      .getComputedStyle
      .toOption
      .map { _ =>
        _underlying.getComputedStyle(elt)
      }
  }

}


/** Интерфейс для window для аккуратного доступа к getComputedStyle(). */
@js.native
sealed trait WndGetComputedStyleStub extends js.Object {
  val getComputedStyle: UndefOr[_] = js.native
}

object WndGetComputedStyleStub {
  def apply(wnd: Window = dom.window): WndGetComputedStyleStub = {
    wnd.asInstanceOf[WndGetComputedStyleStub]
  }
}
