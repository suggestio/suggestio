package io.suggest.sc.sjs.v.vutil

import io.suggest.sjs.common.model.browser.{MBrowser, IBrowser}
import io.suggest.sjs.common.view.safe.{SafeElT, SafeEl}
import org.scalajs.dom
import org.scalajs.dom.{Node, Element}
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}

import scala.annotation.tailrec

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:56
 * Description: Утиль для ускорения сборки view'ов.
 */
object VUtil {

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
                            mbrowser: IBrowser = MBrowser.BROWSER ): Unit = {
    val heightPx = height.toString + "px"
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
      cDiv.style.minHeight = h1.toString + "px"
    }
  }


  def getAttribute(node: Element, name: String): Option[String] = {
    Option( node.getAttribute(name) )
      .filter(!_.isEmpty)
  }

  def getIntAttribute(node: Element, name: String): Option[Int] = {
    getAttribute(node, name)
      .map { _.toInt }
  }

  /** Создать новый div-тег и вернуть его. */
  def newDiv(): HTMLDivElement = {
    dom.document.createElement("div")
      .asInstanceOf[HTMLDivElement]
  }

  /**
   * Определить, если ли у тега или его родителей указанный css-класс.
   * @param el Исходный тег, от которого пляшем.
   * @param className название искомого css-класса.
   * @return Some с узлом, у которой замечен указанный класс. Это el либо один из родительских тегов.
   *         None, если указанный класс не найден у тега и его родителей.
   */
  @tailrec
  def hasCssClass(el: SafeElT, className: String): Option[SafeElT] = {
    if (el.containsClass(className)) {
      Some(el)
    } else {
      val parentEl = el._underlying.parentNode
      if (parentEl == null) {
        None
      } else {
        val parentSafeEl = SafeEl( parentEl )
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
