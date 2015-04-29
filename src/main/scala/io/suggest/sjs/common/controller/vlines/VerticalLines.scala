package io.suggest.sjs.common.controller.vlines

import io.suggest.sjs.common.controller.InitRouter
import org.scalajs.dom.Element
import org.scalajs.jquery.{JQuery, jQuery}
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.04.15 17:01
 * Description: Поддержка ресайза неких вертикальных линий через js.
 * Портировано из mx_cof, хотя до конца непонятно, зачем это.
 */

object VerticalLines {

  def JSVL_CSS_SELECTOR = ".js-vertical-line"

  def INHERNIT_HEIGHT_ATTR = "inherit-height"

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
      val inhOpt = Option( jqel.data(INHERNIT_HEIGHT_ATTR) )
      if ( !inhOpt.contains(true.toString) )
        h -= 10
      jqel.height(h)
    }
  }

}


/** Аддон для init-роутера для активации контроллера инициализации динамических вертикальных линий. */
trait VerticalLinesInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.VCenterLines) {
      Future {
        VerticalLines.resetAllVLinesHeights()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}
