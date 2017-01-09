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

  /** Расширенный Helper с поддержкой безопасного выставления class-аттрибута. */
  protected trait SetterHelper[T] extends Helper[T] with super.SetterHelper[T] {
    // Режимы нужны для передачи результатов логики execute в setAttribute().
    protected def MODE_CLL          = 3

    // Для возможности дополнения логики в setter helper'е используются эти методы.
    override protected def _usingCll(cll: DOMTokenList): T = {
      _mode = MODE_CLL
      super._usingCll(cll)
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


  /**
   * Дописать css-класс, если указанный класс ещё не задан.
   * @param names Названия добавляемых классов.
   */
  def addClasses(names: String*): Unit = {
    val h = new SetterHelper[Unit] {
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
          setAttribute(css1)
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
    val h = new SetterHelper[Unit] {
      override def withClassList(classList: DOMTokenList): Unit = {
        classList.remove(name)
      }
      override def withAttrValue(classAttr: Option[String]): Unit = {
        classAttr.foreach { classAttr1 =>
          val re = _subStrRegex(classAttr1)
          val v = classAttr1.replaceAll(re, " ")
          setAttribute(v)
        }
      }
      override def notFound: Unit = {}
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