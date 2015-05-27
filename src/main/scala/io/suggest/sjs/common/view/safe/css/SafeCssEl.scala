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
  protected trait Helper[T] {
    /** Есть classList. */
    def withClassList(classList: DOMTokenList): T

    /** Нету classList, но возможно есть значение аттрибута. */
    def withAttrValue(classAttr: Option[String]): T

    def execute(): T = {
      val el1 = SafeCssElStub(_underlying)
      val cllOrUndef = el1.classList
      if (cllOrUndef.isDefined) {
        // Std-compilant browser
        val cll = cllOrUndef.get
        withClassList(cll)
      } else {
        // В обход через аттрибуты тега.
        val attr = Option(_underlying.getAttribute("class"))
        withAttrValue( attr )
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
    }
    h.execute()
  }


  /**
   * Дописать css-класс, если указанный класс ещё не задан.
   * @param names Названия добавляемых классов.
   */
  def addClasses(names: String*): Unit = {
    val h = new Helper[Unit] {
      override def withClassList(classList: DOMTokenList): Unit = {
        names.foreach { classList.add }
      }

      override def withAttrValue(classAttr: Option[String]): Unit = {
        val iter = names.iterator
          .filter { name =>
            !_containsClassViaAttr(name, classAttr)
          }
        if (iter.nonEmpty) {
          val css2 = iter.mkString(" ")
          val css1 = classAttr match {
            case None       => css2
            case Some(css0) => css0 + " " + css2
          }
          _underlying.setAttribute("class", css1)
        }
      }
    }
    h.execute()
  }

  /**
   * Удалить css-класс из текущего тега.
   * @param name Название удаляемого класса.
   */
  def removeClass(name: String): Unit = {
    val h = new Helper[Unit] {
      override def withClassList(classList: DOMTokenList): Unit = {
        classList.remove(name)
      }

      override def withAttrValue(classAttr: Option[String]): Unit = {
        classAttr.foreach { classAttr1 =>
          val re = _subStrRegex(classAttr1)
          val v = classAttr1.replaceAll(re, " ")
          _underlying.setAttribute("class", v)
        }
      }
    }
    h.execute()
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