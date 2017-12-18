package io.suggest.sc.sjs.vm.util

import io.suggest.msg.WarnMsgs
import io.suggest.sc.sjs.m.mgrid.MGridState
import io.suggest.sc.sjs.m.msc.IScSd
import io.suggest.sjs.common.log.ILog
import io.suggest.sjs.common.vm.style.{StyleDisplayT, StyleWidth}
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 19:01
 * Description: Система рассчета и выставления offset-размеров сетки для ModelView'ов выдвижных боковых панелей.
 * Извлечен из rebuild_grid, предыдущего поколения выдачи.
 *
 * Трейт подмешивается в ModelView'ы панелей и вызывается из ScFsm для вычисления новых параметров плитки.
 */
trait GridOffsetCalc extends StyleDisplayT with StyleWidth with ILog {

  override type T <: HTMLElement

  /** Константа минимальной ширины колонки. */
  protected def gridOffsetMinWidthPx: Int

  /** Сохранить новый вычисленный cell offset в состояние сетки.
    * Метод должен выставлять rightOffset или leftOffset в mgs, производя новый экземпляр данных
    * состояния сетки. */
  def saveNewOffsetIntoGridState(mgs0: MGridState, newOff: Int): MGridState

  /** Заготовка калькулятора для левой или правой колонки. */
  protected trait GridOffsetterT {

    /** Текущие данные состояние FSM. */
    def sd0: IScSd

    /** Для дедубликации рассчетов дополнительной ширины формула вынесена сюда. */
    def _getWidthAdd: Int = {
      val widthAddOpt = for {
        contSz  <- sd0.grid.state.contSz
      } yield {
        (sd0.common.screen.width - contSz.cw) / 2
      }
      widthAddOpt.getOrElse {
        LOG.warn( WarnMsgs.GRID_CONT_SZ_MISSING )
        0
      }
    }

    /** Размер сдвига в ячейках сетки. */
    def cellOffset = 2

    /**
     * Запустить логику подсчета и расставления параметров на исполнение.
      *
      * @return Пропатченный вариант IGridState.
     */
    def execute(): MGridState = {
      val cellOff = if (isHidden) {
        0
      } else {
        setWidthPx(_getWidthAdd + gridOffsetMinWidthPx)
        cellOffset
      }
      saveNewOffsetIntoGridState(sd0.grid.state, cellOff)
    }

  }

  // TODO Подумать в сторону value-class реализации.

  /** Дефолтовая реализация калькулятора сетки в рамках текущей vm'ки. */
  class GridOffsetter(override val sd0: IScSd)
    extends GridOffsetterT

  def GridOffsetter(sd0: IScSd): GridOffsetter = {
    new GridOffsetter(sd0)
  }

}
