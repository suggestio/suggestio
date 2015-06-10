package io.suggest.sc.sjs.m.mgrid

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.tile.ColumnsCountT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:45
 * Description: Модель состояния cbca_grid.
 */
object MGrid {

  /** Параметры, выставляются всей пачкой. */
  var params: MGridParams = MGridParams()

  /** Текущее состояние сетки. Состоит из переменных и обновляется контроллерами. */
  var state: MGridState = _


  /** Произвести сброс state. */
  def resetState(): Unit = {
    state = new MGridState()
  }

  def margin(colCnt1: Int): Int = {
    val p = params
    val cs = p.cellSize
    (colCnt1 - 1) * (cs + p.cellPadding) + cs
  }


  /**
   * Посчитать кол-во колонок сетки с помощью калькулятора колонок.
   * @return Кол-во колонок сетки на экране.
   */
  def countColumns(screen: ISize2di = MAgent.availableScreen): Int = {
    val calc = new ColumnsCountT {
      override def CELL_WIDTH_CSSPX   = params.cellSize
      override def TILE_PADDING_CSSPX = params.cellPadding
    }
    calc.getTileColsCountScr(screen)
  }



  /**
   * Рассчет новых параметров контейнера.
   * Это pure-function, она не меняет состояние системы, а только считает.
   * @return Экземпляр с результатами рассчетов.
   */
  def getContainerSz(): MGetContStateResult = {
    val colCount = countColumns()

    var cw = margin(colCount)
    var cm = 0
    var _margin = 0

    val leftOff = state.leftOffset
    if (leftOff > 0) {
      _margin = MGrid.margin( leftOff )
      cw = cw - _margin
      cm = _margin
    }

    val rightOff = state.rightOffset
    if (rightOff > 0) {
      _margin = MGrid.margin( rightOff )
      cw = cw - _margin
      cm = -_margin
    }

    MGetContStateResult(
      cw = cw,
      cm = cm,
      maxCellWidth = colCount,
      columnsCnt   = colCount - leftOff - rightOff
    )
  }


}

