package io.suggest.sjs.common.view

import io.suggest.common.css.CssSzImplicits
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.vm.create.CreateDiv
import io.suggest.sjs.common.vm.{Vm, VmT}
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}
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


  /**
   * Выставить указанную высоту на цепочках контейнеров с учетом возможных костылей для скроллинга.
   * @param height Высота.
   * @param content content div, если есть.
   * @param wrappers div'ы, заворачивающие content div.
   * @param mbrowser Закешированный браузер, если есть.
   */
  def setHeightRootWrapCont(height: Int,
                            content: Option[HTMLElement],
                            wrappers: TraversableOnce[HTMLElement] = Nil,
                            mbrowser: IBrowser): Unit = {
    val heightPx = height.px
    // Отрабатываем враппер-контейнеры.
    for (wrapper <- wrappers) {
      //if (!needXScroll)
      //  SafeEl(wrapper).removeClass(ScConstants.OVERFLOW_VSCROLL_CSS_CLASS)
      wrapper.style.height = heightPx
    }
    // Отрабатываем основной контейнер.
    for (cDiv <- content) {
      // Нужно ли провоцировать скроллбар в цепочке контейнеров? Да, если браузер работает так.
      val needXScroll = mbrowser.needOverwriteInnerScroll
      val h1 = if (needXScroll)  height + 1  else  height
      cDiv.style.minHeight = h1.px
    }
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
   * Определить, если ли у тега или его родителей указанный css-класс.
   * @param el Исходный тег, от которого пляшем.
   * @param className название искомого css-класса.
   * @return Some с узлом, у которой замечен указанный класс. Это el либо один из родительских тегов.
   *         None, если указанный класс не найден у тега и его родителей.
   */
  @tailrec
  def hasCssClass(el: VmT, className: String): Option[VmT] = {
    if (el.containsClass(className)) {
      Some(el)
    } else {
      val parentEl = el._underlying.parentNode
      if (parentEl == null) {
        None
      } else {
        val parentSafeEl = Vm( parentEl )
        hasCssClass(parentSafeEl, className)
      }
    }
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
