package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.sjs.m.magent.MAgent
import org.scalajs.dom.raw.HTMLElement


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 19:01
 * Description: Калькулятор для рассчета offset-размеров сетки.
 * Извлечен из rebuild_grid, предыдущего поколения выдачи.
 */

trait GridOffsetCalc {

  /** Если [[MGridState]] указывает на необходимость нулевого оффсета, то её следует послушать. */
  def canNonZeroOffset: Boolean

  /** div-элемент текущей панели, с которым работаем. */
  def elOpt: Option[HTMLElement]

  def minWidth: Int

  def widthAdd: Int

  def calculate(): Int = {
    if (canNonZeroOffset) {
      elOpt match {
        case Some(el) =>
          val disp = el.style.display
          val isHidden = disp.isEmpty || disp == "none"
          if (isHidden) {
            0
          } else {
            // TODO Тут был кусок view'а:   sm_geo_screen.style.width = 280 + Math.round((cbca_grid.ww - parseInt(cbca_grid.cw)) / 2)
            2
          }

        // should never happen
        case None =>
          0
      }

    } else {
      0
    }
  }

}

object GridOffsetCalc {

  def widthAdd(cw: Int, wndWidth: Int = MAgent.availableScreen.width): Int = {
    (wndWidth - cw) / 2
  }

}