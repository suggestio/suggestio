package io.suggest.sc.sjs.util.grid

import io.suggest.sc.sjs.m.IAppState
import io.suggest.sc.sjs.m.mgrid.{MGetContStateResult, MGridState}
import io.suggest.sc.tile.ColumnsCountT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:28
 * Description: Сетка плитки, т.е. ядро логики cbca_grid.
 *
 * Некоторые части исходной cbca_grid вынесены в отдельные модули.
 * @see [[io.suggest.sc.sjs.v.render.direct.vport.sz.ViewportSzImpl]] Детектор размера viewport'а.
 */

object Grid {

  /**
   * Посчитать колонки с помощью калькулятора.
   * @param mgs
   * @param appState
   * @return
   */
  def countColumns(mgs: MGridState)(implicit appState: IAppState): Int = {
    val calc = new ColumnsCountT {}
    calc.getTileColsCountScr(appState.agent.availableScreen)
  }

  /**
   * Рассчет новых параметров контейнера.
   * view должен накатить рассчеты на DOM и обновить состояние.
   * @param mgs Исходное состояние сетки.
   * @param appState Состояние приложения.
   * @return Экземпляр с результатами рассчетов.
   */
  def getContainerSz(mgs: MGridState)(implicit appState: IAppState): MGetContStateResult = {
    val colCount = countColumns(mgs)

    var cw = mgs.margin(colCount)
    var cm = 0
    var margin = 0

    val leftOff = mgs.leftOffset
    if (leftOff > 0) {
      margin = mgs.margin( leftOff )
      cw = cw - margin
      cm = margin
    }
    
    val rightOff = mgs.rightOffset
    if (rightOff > 0) {
      margin = mgs.margin( rightOff )
      cw = cw - margin
      cm = -margin
    }

    /*
    val width = cw.toString + "px"

    layout.style.width    = width
    layout.style.left     = (cm/2).toString + "px"
    layout.style.opacity  = "1"
    
    loader.style.width    = width
    */

    MGetContStateResult(
      cw = cw,
      cm = cm,
      mgs1 = mgs.copy(
        maxCellWidth = colCount,
        columnsCnt   = colCount - leftOff - rightOff
      )
    )
  }


}
