package io.suggest.sc.sjs.v.vutil

import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}

import scala.scalajs.js

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

}
