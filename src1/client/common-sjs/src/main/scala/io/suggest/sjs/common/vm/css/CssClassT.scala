package io.suggest.sjs.common.vm.css

import java.util.regex.Pattern

import io.suggest.sjs.common.vm.attr.AttrVmUtilT
import org.scalajs.dom.Node
import org.scalajs.dom.raw.{NamedNodeMap, DOMTokenList}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 14:21
 * Description: Враппер для безопасного доступа к управлению css-классами тега.
 */

trait CssClassT extends AttrVmUtilT {

  override type T <: Node

  protected def _subStrRegex(name: String): String = {
    "(^|.*\\s)" + Pattern.quote(name) + "(\\s.*|$)"
  }

  protected def _containsClassViaAttr(name: String, css0: Option[String]): Boolean = {
    css0
      .filter { !_.isEmpty }
      .exists { css =>
        css.matches( _subStrRegex(name) )
      }
  }

}


/** Интерфейс для необязательно доступного свойства Element.classList. */
@js.native
sealed trait ElCssClassStub extends js.Object {
  var classList: UndefOr[DOMTokenList] = js.native

  def getAttribute: UndefOr[js.Function1[String, String]] = js.native

  def attributes: UndefOr[NamedNodeMap] = js.native
}


/** Поддержка приведения Element к SafeCssClass. */
object ElCssClassStub {

  @inline def apply(el: Node): ElCssClassStub = {
    el.asInstanceOf[ElCssClassStub]
  }

}