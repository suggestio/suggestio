package io.suggest.sc.sjs.m.mgrid

import io.suggest.adv.ext.model.im.ISize2di

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 17:45
 * Description: Модель состояния cbca_grid.
 */
object MGrid extends MGridUtil {

  var gridParams: MGridParams = MGridParams()

  var gridState: MGridState = _

  /** Произвести сброс state. */
  def resetState(): Unit = {
    gridState = new MGridState()
  }

}


/** Утиль для анализа данных в grid-моделях. */
trait MGridUtil {

  /** Параметры сетки, выставляются всей пачкой. */
  def gridParams: MGridParams

  /** Текущее состояние сетки. Состоит из переменных и обновляется контроллерами. */
  def gridState: MGridState

  /**
   * Рассчет новых параметров контейнера.
   * Это pure-function, она не меняет состояние системы, а только считает.
   * @param screen Данные по текущему экрану.
   * @return Экземпляр с результатами рассчетов.
   */
  def getGridContainerSz(screen: ISize2di): MGetContStateResult = {
    val colCount = gridParams.countColumns(screen)

    var cw = gridParams.margin(colCount)
    var cm = 0
    var _margin = 0

    val leftOff = gridState.leftOffset
    if (leftOff > 0) {
      _margin = gridParams.margin( leftOff )
      cw = cw - _margin
      cm = _margin
    }

    val rightOff = gridState.rightOffset
    if (rightOff > 0) {
      _margin = gridParams.margin( rightOff )
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
