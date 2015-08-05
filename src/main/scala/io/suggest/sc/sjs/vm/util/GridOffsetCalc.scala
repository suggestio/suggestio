package io.suggest.sc.sjs.vm.util

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msc.fsm.IStData
import io.suggest.sjs.common.view.safe.css.Width
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
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
trait GridOffsetCalc extends SetDisplayEl with Width {

  override type T <: HTMLElement

  /** Заготовка калькулятора для левой или правой колонки. */
  protected trait GridOffsetterT {

    /** Текущие данные состояние FSM. */
    def sd0: IStData

    /** Для дедубликации рассчетов дополнительной ширины формула вынесена сюда. */
    def _getWidthAdd: Int = {
      (screen.width - mgs.contSz.get.cw) / 2
    }

    /** Состояние сетки, передаётся из состояния FSM. */
    protected def mgs = sd0.grid.state

    /** Данные по экрану устройства. */
    def screen: ISize2di

    /** Если mgs указывает на необходимость нулевого оффсета, то её следует послушать. */
    def canNonZeroOffset: Boolean = mgs.canNonZeroOffset

    /** Константа минимальной ширины колонки. */
    def minWidth: Int

    /** Сохранить новый cell offset в состояние. Метод должен выставлять rightOffset или leftOffset в mgs. */
    def setOffset(newOff: Int): MGridState

    /** Размер сдвига в ячейках сетки. */
    def cellOffset = 2

    /**
     * Запустить логику подсчета и расставления параметров на исполнение.
     * @return Пропатченный вариант IGridState.
     */
    def apply(): MGridState = {
      val res = canNonZeroOffset && {
        !isHidden && {
          setWidthPx(_getWidthAdd + minWidth)
          true
        }
      }
      val cellOff = if (res) cellOffset else 0
      setOffset(cellOff)
    }

  }

}
