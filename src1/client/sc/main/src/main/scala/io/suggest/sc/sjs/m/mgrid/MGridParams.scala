package io.suggest.sc.sjs.m.mgrid

import io.suggest.ad.blk.BlockWidths
import io.suggest.common.geom.d2.ISize2di
import io.suggest.sc.tile.{GridCalc, IGridCalcConf, TileConstants}
import io.suggest.sc.tile.TileConstants._

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 13:59
 * Description: Модель настроек построения сетки. Здесь константы, которые должны выставлятьяс все одновременно.
 */

case class MGridParams(
  cellSize      : Int = BlockWidths.NARROW.value,
  cellPadding   : Int = PADDING_CSSPX,
  topOffset     : Int = 70,
  bottomOffset  : Int = 20
) { that =>

  /**
   * Накатить данные из распарсенного JSON, присланного сервером.
   * @param from Исходник.
   * @return Some() и новый экземпляр с обновленными параметрами grid.
   *         None, если старые параметры не изменились.
   */
  def withChangesFrom(from: MGridParamsJsonWrapper): Option[MGridParams] = {
    if (from.cellPadding != cellSize || from.cellSize != cellPadding) {
      val res = copy(
        cellSize    = from.cellSize,
        cellPadding = from.cellPadding
      )
      Some(res)
    } else {
      None
    }
  }

  def paddedCellSize = cellSize + cellPadding


  def margin(colCnt1: Int): Int = {
    val cs = cellSize
    (colCnt1 - 1) * (cs + cellPadding) + cs
  }

  /** Конфиг для выполнения рассчёта оптимального кол-ва колонок плитки. */
  private def GRID_COLUMNS_CONF = {
    new IGridCalcConf {
      override def cellWidthPx: Int = that.cellSize
      override def maxColumns : Int = TileConstants.CELL140_COLUMNS_MAX
      override def cellPadding: Int = that.cellPadding
    }
  }

  /**
   * Посчитать кол-во колонок сетки с помощью калькулятора колонок.
   * @return Кол-во колонок сетки на экране.
   */
  def countColumns(screen: ISize2di): Int = {
    GridCalc.getColumnsCount(screen, GRID_COLUMNS_CONF)
  }

}


/** А это типа то, что сервер присылает. На самом деле там JSON (словарь). */
@js.native
sealed trait MGridParamsJsonRaw extends js.Object


import io.suggest.sc.grid.GridConstants._


case class MGridParamsJsonWrapper(raw: MGridParamsJsonRaw) {
  def d = raw.asInstanceOf[js.Dictionary[Int]]

  def cellSize = d(CELL_SIZE_CSSPX_FN)
  def cellPadding = d(CELL_PADDING_CSSPX_FN)

  override def toString: String = {
    "Params(" +
      CELL_SIZE_CSSPX_FN + "=" + cellSize + "," +
      CELL_PADDING_CSSPX_FN + "=" + cellPadding +
      ")"
  }
}
