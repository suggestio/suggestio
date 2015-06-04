package io.suggest.sc.sjs.v.vutil

import io.suggest.sc.ScConstants
import io.suggest.sjs.common.model.browser.{MBrowser, IBrowser}
import io.suggest.sjs.common.view.safe.SafeEl
import io.suggest.sjs.common.view.safe.css.{SafeCssElT, SafeCssEl}
import org.scalajs.dom
import org.scalajs.dom.{Node, Element}
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}

import scala.annotation.tailrec
import scala.scalajs.js

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
    // Нужно ли провоцировать скроллбар в цепочке контейнеров? Да, если браузер работает так.
    val needXScroll = mbrowser.needOverwriteInnerScroll
    // Отрабатываем враппер-контейнеры.
    for (wrapper <- wrappers) {
      if (!needXScroll)
        SafeEl(wrapper).removeClass(ScConstants.OVERFLOW_VSCROLL_CSS_CLASS)
      wrapper.style.height = heightPx
    }
    // Отрабатываем основной контейнер.
    for (cDiv <- content) {
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
   * Извлечь целое число из строки, если оно там есть.
   * @param s Строка, содержащая число, "100px" например.
   * @param radix Основание системы счисления.
   * @see [[https://github.com/scala-js/scala-js/blob/master/javalanglib/src/main/scala/java/lang/Integer.scala#L65 По мотивам Integer.parseInt()]]
   * @return Целое, если найдено.
   */
  def extractInt(s: String, radix: Int = 10): Option[Int] = {
    Option(s)
      .filter { !_.isEmpty }
      .flatMap { s1 =>
        val res = js.Dynamic.global.parseInt(s1, radix)
          .asInstanceOf[scala.Double]
        if (res.isNaN || res > Integer.MAX_VALUE || res < Integer.MIN_VALUE) {
          None
        } else {
          Some(res.toInt)
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
  @tailrec
  def hasCssClass(el: SafeCssElT, className: String): Option[SafeCssElT] = {
    if (el.containsClass(className)) {
      Some(el)
    } else {
      val parentEl = el._underlying.parentNode
      if (parentEl == null) {
        None
      } else {
        val parentSafeEl = SafeCssEl( parentEl )
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
