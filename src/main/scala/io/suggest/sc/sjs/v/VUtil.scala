package io.suggest.sc.sjs.v

import org.scalajs.dom.raw.HTMLElement

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

}
