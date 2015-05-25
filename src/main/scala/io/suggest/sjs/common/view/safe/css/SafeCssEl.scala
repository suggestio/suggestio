package io.suggest.sjs.common.view.safe.css

import java.util.regex.Pattern

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.Element
import org.scalajs.dom.raw.DOMTokenList

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 14:21
 * Description: Враппер для безопасного доступа к управлению css-классами тега.
 */

trait SafeCssElT extends ISafe {

  override type T <: Element


  protected def getCssClass(): Option[String] = {
    Option( _underlying.getAttribute("class") )
  }

  protected def _containsClassViaAttr(name: String, css0: Option[String] = getCssClass()): Boolean = {
    css0
      .filter { !_.isEmpty }
      .exists { css =>
        val regex = "(^|.*\\s)" + Pattern.quote(name) + "(\\s.*|$)"
        css.matches(regex)
      }
  }


  /**
   * Содержится ли указанный css-класс у текущего элемента?
   * @param name Название css-класса.
   * @return true, если у элемента уже есть css-класс с указанными именем.
   *         Иначе false.
   */
  def containsClass(name: String): Boolean = {
    val el1 = SafeCssElStub(_underlying)
    val cllOrUndef = el1.classList
    if (cllOrUndef.isDefined) {
      // Std-compilant browser
      val cll = cllOrUndef.get
      cll.contains(name)
    } else {
      // В обход через аттрибуты тега.
      _containsClassViaAttr(name)
    }
  }

  /**
   * Дописать css-класс, если указанный класс ещё не задан.
   * @param names Названия добавляемых классов.
   */
  def addClasses(names: String*): Unit = {
    val el = _underlying
    val el1 = SafeCssElStub(el)
    val cllOrUndef = el1.classList
    if (cllOrUndef.isDefined) {
      // Стандартный браузер.
      val cll = cllOrUndef.get
      names.foreach { name =>
        cll.add(name)
      }
    } else {
      // В обход через аттрибуты тега.
      val oldCssOpt = getCssClass()
      val iter = names.iterator
        .filter { name =>
          !_containsClassViaAttr(name, oldCssOpt)
        }
      if (iter.nonEmpty) {
        val css2 = iter.mkString(" ")
        val css1 = oldCssOpt match {
          case None       => css2
          case Some(css0) => css0 + " " + css2
        }
        el.setAttribute("class", css1)
      }
    }
  }

}


/** Дефолтовая реализация [[SafeCssElT]]. */
case class SafeCssEl(_underlying: Element) extends SafeCssElT {
  override type T = Element
}


/** Интерфейс для необязательно доступного свойства Element.classList. */
trait SafeCssElStub extends js.Object {
  var classList: UndefOr[DOMTokenList] = js.native
}


/** Поддержка приведения Element к SafeCssClass. */
object SafeCssElStub {

  @inline def apply(el: Element): SafeCssElStub = {
    el.asInstanceOf[SafeCssElStub]
  }

}