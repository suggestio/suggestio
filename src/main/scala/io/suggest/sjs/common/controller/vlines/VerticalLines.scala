package io.suggest.sjs.common.controller.vlines

import io.suggest.sjs.common.controller.{InitController, InitRouter}
import io.suggest.sjs.common.util.SjsLogger
import org.scalajs.dom.Element
import org.scalajs.jquery.{JQuery, jQuery}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.04.15 17:01
 * Description: Поддержка ресайза неких вертикальных линий через js.
 * Портировано из mx_cof, хотя до конца непонятно, зачем это.
 */

object VerticalLines {

  def JSVL_CSS_SELECTOR = ".js-vertical-line"

  def INHERNIT_HEIGHT_ATTR = "data-inherit-height"

  def resetVLinesHeightsIn(parent: JQuery): Unit = {
    val sel = parent.find(JSVL_CSS_SELECTOR)
    resetVLinesHeight(sel)
  }

  def resetAllVLinesHeights(): Unit = {
    val sel = jQuery(JSVL_CSS_SELECTOR)
    resetVLinesHeight(sel)
  }

  def resetVLinesHeight(lines: JQuery): Unit = {
    lines.each { (index: Any, el: Element) =>
      val jqel = jQuery(el)
      var h = jqel.parent().height()
      // Если не выставлен data-inherit-height, то надо немного уменьшить высоту.
      if ( !(Option(jqel.attr(INHERNIT_HEIGHT_ATTR)) contains true.toString) )
        h -= 10
      jqel.height(h)
    }
  }

}


/** Аддон для init-роутера для активации контроллера инициализации динамических вертикальных линий. */
trait VerticalLinesInitRouter extends InitRouter {

  /** Текущий init-роутер выполняет поиск init-контроллера с указанным именем (ключом).
    * Реализующие трейты должны переопределять этот метод под себя, сохраняя super...() вызов. */
  override protected def getController(name: String): Option[InitController] = {
    if (name == "_VerticalLines") {
      Some(new VerticalLinesInitController)
    } else {
      super.getController(name)
    }
  }

}

/** init-контролер для инициализации поддержки вертикальных линий. */
class VerticalLinesInitController extends InitController with SjsLogger {
  override def riInit(): Unit = {
    super.riInit()
    VerticalLines.resetAllVLinesHeights()
  }
}
