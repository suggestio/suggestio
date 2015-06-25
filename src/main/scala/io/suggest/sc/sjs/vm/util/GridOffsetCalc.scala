package io.suggest.sc.sjs.vm.util

import io.suggest.adv.ext.model.im.{ISize2di, IWidth}
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.sjs.m.mgrid.MGridState
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 19:01
 * Description: Система рассчета и выставления offset-размеров сетки для ModelView'ов выдвижных боковых панелей.
 * Извлечен из rebuild_grid, предыдущего поколения выдачи.
 *
 * Трейт подмешивается в ModelView'ы панелей и дореализовывается в рамках специфики конкретной панели.
 */
trait GridOffsetCalc {

  /** Для дедубликации рассчетов дополнительной ширины формула вынесена сюда. */
  protected def getWidthAdd(mgs: MGridState, wndWidth: IWidth): Int = {
    (wndWidth.width - mgs.contSz.get.cw) / 2
  }

  /** Заготовка калькулятора для левой или правой колонки. */
  protected trait GridOffsetterT {

    /** Состояние сетки, передаётся из состояния FSM. */
    def mgs: MGridState

    /** Данные по экрану устройства. */
    def screen: ISize2di

    /** Если [[MGridState]] указывает на необходимость нулевого оффсета, то её следует послушать. */
    def canNonZeroOffset: Boolean = mgs.canNonZeroOffset

    /** div-элемент текущей панели, с которым работаем. */
    def elOpt: Option[HTMLElement]

    /** Константа минимальной ширины колонки. */
    def minWidth: Int

    /** Константа widthAdd, посчитанная через getWidthAdd(). */
    def widthAdd: Int = getWidthAdd(mgs, screen)

    def isElHidden(el: HTMLElement): Boolean = {
      val disp = el.style.display
      disp.isEmpty || disp == "none"
    }

    /** Сохранить новый cell offset в состояние. */
    def setOffset(newOff: Int): MGridState

    def setWidth(el: HTMLElement): Unit = {
      el.style.width = (minWidth + widthAdd) + "px"
    }

    /** Размер сдвига в ячейках сетки. */
    def cellOffset = 2

    /**
     * Запустить логику подсчета и расставления параметров на исполнение.
     * @return Пропатченный вариант MGridState.
     */
    def apply(): MGridState = {
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
