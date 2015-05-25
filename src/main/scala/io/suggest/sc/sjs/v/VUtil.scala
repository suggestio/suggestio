package io.suggest.sc.sjs.v

import io.suggest.adv.ext.model.im.{ISize2di, Size2di}
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.raw.{HTMLImageElement, HTMLElement}

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

  /** Быстро и кратко записывать получение элементов из DOM по id. */
  def getElementById[T <: Element](id: String): Option[T] = {
    val elOrNull = dom.document
      .getElementById(id)
      .asInstanceOf[T]
    Option(elOrNull)
  }


  def getAttribute(node: Element, name: String): Option[String] = {
    Option( node.getAttribute(name) )
      .filter(!_.isEmpty)
  }

  def getIntAttribute(node: Element, name: String): Option[Int] = {
    getAttribute(node, name)
      .map { _.toInt }
  }

}


/** View-утиль для взаимодейтсвия с тегами картинок. */
object VImgUtil {

  /** Прочитать значение из аттрибута data-width. */
  def readDataWidth(el: Element) = VUtil.getIntAttribute(el, "data-width")
  /** Прочитать значение из аттрибута data-height. */
  def readDataHeight(el: Element) = VUtil.getIntAttribute(el, "data-height")

  /** Прочитать данные о размерах из data-{width, height}. */
  def readDataWh(el: Element): Option[Size2di] = {
    for {
      width  <- readDataWidth(el)
      height <- readDataHeight(el)
    } yield {
      Size2di(width = width, height = height)
    }
  }

  /** Выставить новые отображаемые размеры для картинки и margin-left. */
  def setImageWh(el: HTMLImageElement, wh: ISize2di): Unit = {
    el.style.width = wh.width + "px"
    el.style.height = wh.height + "px"
  }

}

