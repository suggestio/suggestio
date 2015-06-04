package io.suggest.sjs.common.view.safe.css

import java.util.regex.Pattern

import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.{Node, Element}
import org.scalajs.dom.raw.{NamedNodeMap, DOMTokenList}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 14:21
 * Description: Враппер для безопасного доступа к управлению css-классами тега.
 */

trait SafeCssElT extends ISafe {

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
  protected trait Helper[T] {

    /** Есть classList. */
    def withClassList(classList: DOMTokenList): T

    /** Нету classList, но возможно есть значение аттрибута. */
    def withAttrValue(classAttr: Option[String]): T

    /** Не найдено способа получения значения аттрибута. */
    def notFound: T = throw new NoSuchElementException("css n/a")

    // Для возможности дополнения логики в setter helper'е используются эти методы.
    protected def _usingCll(cll: DOMTokenList): T = {
      withClassList(cll)
    }
    
    protected def _usingElGetAttrValue(attrValue: Option[String]): T = {
      withAttrValue(attrValue)
    }

    protected def _usingNodeAttrsGetValue(attrValue: Option[String]): T = {
      withAttrValue(attrValue)
    }
    
    def execute(): T = {
      val el1 = SafeCssElStub(_underlying)
      val cllOrUndef = el1.classList

      if (cllOrUndef.isDefined) {
        // Std-compilant browser
        _usingCll(cllOrUndef.get)

      } else {
        val maybeGetAttrF = el1.getAttribute
        val attrName = "class"
        if (maybeGetAttrF.nonEmpty) {
          // В обход через аттрибуты тега.
          val attrValue = Option( maybeGetAttrF.get.apply(attrName) )
          _usingElGetAttrValue(attrValue)

        } else {
          val maybeAttrs = el1.attributes
          if (maybeAttrs.nonEmpty) {
            val attrValue = Option( maybeAttrs.get.getNamedItem(attrName) )
              .flatMap(attr => Option(attr.value))
            _usingNodeAttrsGetValue(attrValue)

          } else {
            notFound
          }
        }
      }
    }
  }

  /** Расширенный Helper с поддержкой безопасного выставления class-аттрибута. */
  protected trait SetterHelper[T] extends Helper[T] {
    // Режимы нужны для передачи результатов логики execute в setAttribute().
    protected def MODE_UNKNOWN      = 0
    protected def MODE_CLL          = 1
    protected def MODE_EL_GET_ATTR  = 2
    protected def MODE_NODE_ATTRS   = 3

    protected var _mode: Int = MODE_UNKNOWN

    // Для возможности дополнения логики в setter helper'е используются эти методы.
    override protected def _usingCll(cll: DOMTokenList): T = {
      _mode = MODE_CLL
      super._usingCll(cll)
    }

    override protected def _usingElGetAttrValue(attrValue: Option[String]): T = {
      _mode = MODE_EL_GET_ATTR
      super._usingElGetAttrValue(attrValue)
    }

    override protected def _usingNodeAttrsGetValue(attrValue: Option[String]): T = {
      _mode = MODE_NODE_ATTRS
      super._usingNodeAttrsGetValue(attrValue)
    }

    /** Безопасно выставить аттрибут class для текущего тега/узла, когда classList недоступен. */
    def setAttribute(v: String): Unit = {
      if (_mode == MODE_EL_GET_ATTR) {
        _underlying.asInstanceOf[Element].setAttribute("class", v)
      } else if (_mode == MODE_NODE_ATTRS) {
        _underlying.attributes.getNamedItem("class").value = v
      } else {
        notFound
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


/** Дефолтовая реализация [[SafeCssElT]]. */
case class SafeCssEl(_underlying: Node) extends SafeCssElT {
  override type T = Node
}


/** Интерфейс для необязательно доступного свойства Element.classList. */
trait SafeCssElStub extends js.Object {
  var classList: UndefOr[DOMTokenList] = js.native

  def getAttribute: UndefOr[js.Function1[String, String]] = js.native

  def attributes: UndefOr[NamedNodeMap] = js.native
}


/** Поддержка приведения Element к SafeCssClass. */
object SafeCssElStub {

  @inline def apply(el: Node): SafeCssElStub = {
    el.asInstanceOf[SafeCssElStub]
  }

}