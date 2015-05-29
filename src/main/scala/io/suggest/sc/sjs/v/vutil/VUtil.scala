package io.suggest.sc.sjs.v.vutil

import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 15:56
 * Description: Утиль для ускорения сборки view'ов.
 */
object VUtil {

  def setHeightRootWrapCont(height: Int, content: Option[HTMLElement], wrappers: TraversableOnce[HTMLElement] = Nil): Unit = {
    val heightPx = height.toString + "px"
    wrappers.foreach { wrapper =>
      wrapper.style.height = heightPx
    }

    content.foreach { cDiv =>
      cDiv.style.minHeight = (height + 1).toString + "px"
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

}
