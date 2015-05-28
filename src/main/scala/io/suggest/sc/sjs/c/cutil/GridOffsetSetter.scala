package io.suggest.sc.sjs.c.cutil

import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid.{MGrid, MGridState}
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 19:01
 * Description: Система рассчета и выставления offset-размеров сетки. Вызывается из контроллера.
 * Извлечен из rebuild_grid, предыдущего поколения выдачи.
 */

trait GridOffsetSetter {

  protected def getWidthAdd(mgs: MGridState = MGrid.state, wndWidth: Int = MAgent.availableScreen.width): Int = {
    (wndWidth - mgs.contSz.get.cw) / 2
  }

  /** Заготовка калькулятора для левой или правой колонки. */
  protected trait GridOffsetCalc {

    /** Если [[MGridState]] указывает на необходимость нулевого оффсета, то её следует послушать. */
    def canNonZeroOffset: Boolean

    /** div-элемент текущей панели, с которым работаем. */
    def elOpt: Option[HTMLElement]

    /** Константа минимальной ширины колонки. */
    def minWidth: Int

    /** Константа widthAdd, посчитанная через getWidthAdd(). */
    def widthAdd: Int

    def isElHidden(el: HTMLElement): Boolean = {
      val disp = el.style.display
      disp.isEmpty || disp == "none"
    }

    /** Сохранить новый cell offset в состояние. */
    def setOffset(newOff: Int): Unit

    def setWidth(el: HTMLElement): Unit = {
      el.style.width = (minWidth + widthAdd) + "px"
    }

    /** Размер сдвига в ячейках сетки. */
    def cellOffset = 2

    /** Запустить логику подсчета и расставления параметров на исполнение. */
    def execute(): Unit = {
      val res = canNonZeroOffset && {
        val _elOpt = elOpt
        _elOpt.nonEmpty && {
          val el = _elOpt.get
          !isElHidden(el) && {
            setWidth(el)
            true
          }
        }
      }
      val cellOff = if (res) cellOffset else 0
      setOffset(cellOff)
    }

  }

}
