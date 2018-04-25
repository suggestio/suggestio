package io.suggest.sjs.common.view

import io.suggest.common.css.CssSzImplicits
import io.suggest.sjs.common.vm.create.CreateDiv
import io.suggest.sjs.common.vm.{Vm, VmT}
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement
import org.scalajs.dom.{Element, Node}

import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:56
 * Description: Утиль для ускорения сборки view'ов.
 */
object VUtil extends CssSzImplicits with CreateDiv {

  def getElementByIdOrNull[T <: Element](id: String): T = {
    dom.document
      .getElementById(id)
      .asInstanceOf[T]
  }

  /**
   * Быстро и кратко записывать получение элементов из DOM по id.
   * @param id id элемента.
   * @tparam T Тип возвращаемого элемента, если найден.
   * @return Опционально-найденный элемент DOM.
   */
  def getElementById[T <: Element](id: String): Option[T] = {
    Option( getElementByIdOrNull[T](id) )
  }


  /** Создать новый div-тег и вернуть его. */
  def newDiv() = createNewEl()

  /** Создать новый тег с указанным содержимым. */
  def newDiv(innerHtml: String): HTMLDivElement = {
    val e = newDiv()
    e.innerHTML = innerHtml
    e
  }


  /**
   * Поиск первого элемента вверх по дереву, удовлетворяющего предикату.
   * @param el VM начального элемента.
   * @param f Предикат.
   * @return Some, если что-то найдено.
   */
  @tailrec
  def hasOrHasParent(el: VmT)(f: VmT => Boolean): Option[VmT] = {
    if (dom.document == el._underlying) {
      None
    } else if ( f(el) ) {
      Some(el)
    } else {
      val parentEl = el._underlying.parentNode
      if (parentEl == null) {
        None
      } else {
        val parentSafeEl = Vm( parentEl )
        hasOrHasParent(parentSafeEl)(f)
      }
    }
  }

  /**
   * Определить, если ли у тега или его родителей указанный css-класс.
   * @param el Исходный тег, от которого пляшем.
   * @param className название искомого css-класса.
   * @return Some с узлом, у которой замечен указанный класс. Это el либо один из родительских тегов.
   *         None, если указанный класс не найден у тега и его родителей.
   */
  def hasCssClass(el: VmT, className: String): Option[VmT] = {
    hasOrHasParent(el)(_.containsClass(className))
  }


  /**
   * Быстро удалить все дочерние элементы, отвязав event listener'ы от них.
   * @param node Родительский узел.
   */
  @tailrec
  def removeAllChildren(node: Node): Unit = {
    val fcOrNull = node.firstChild
    if (fcOrNull != null) {
      node.removeChild(fcOrNull)
      removeAllChildren(node)
    }
  }

}
