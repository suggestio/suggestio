package io.suggest.sjs.common.view

import io.suggest.common.css.CssSzImplicits
import io.suggest.sjs.common.vm.create.CreateDiv
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
