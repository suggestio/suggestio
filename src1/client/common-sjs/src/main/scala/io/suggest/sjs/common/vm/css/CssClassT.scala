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

  /** Вспомогательная подсистема для отработки разных вариантов.  */
  protected trait Helper[T] extends super.Helper[T] {

    override def attrName = "class"

    /** Есть classList. */
    def withClassList(classList: DOMTokenList): T

    // Для возможности дополнения логики в setter helper'е используются эти методы.
    protected def _usingCll(cll: DOMTokenList): T = {
      withClassList(cll)
    }
    
    override def execute(): T = {
      val el1 = ElCssClassStub(_underlying)
      val cllOrUndef = el1.classList

      if (cllOrUndef.isDefined) {
        // Std-compilant browser
        _usingCll(cllOrUndef.get)
      } else {
        super.execute()
      }
    }
  }


  /**
   * Содержится ли указанный css-класс у текущего элемента?
   * @param name Название css-класса.
   * @return true, если у элемента уже есть css-класс с указанными именем.
   *         Иначе false.
   */
  def containsClass(name: String): Boolean = {
    // Собираем helper для проверки наличия класса.
    val h = new Helper[Boolean] {
      override def withClassList(classList: DOMTokenList): Boolean = {
        classList contains name
      }
      override def withAttrValue(classAttr: Option[String]): Boolean = {
        _containsClassViaAttr(name, classAttr)
      }
      override def notFound = false
    }
    h.execute()
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