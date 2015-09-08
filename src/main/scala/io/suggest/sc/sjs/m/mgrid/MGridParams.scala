package io.suggest.sc.sjs.m.mgrid

import io.suggest.common.geom.d2.ISize2di
import io.suggest.sc.tile.ColumnsCountT
import io.suggest.sc.tile.TileConstants._

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 13:59
 * Description: Модель настроек построения сетки. Здесь константы, которые должны выставлятьяс все одновременно.
 */

object MGridParams {

  // Этот параметр используется при инициализации при вешаньи события, поэтому он неизменяемый.
  def LOAD_MORE_SCROLL_DELTA_PX = 100

}


case class MGridParams(
  cellSize      : Int = CELL_WIDTH_140_CSSPX,
  cellPadding   : Int = PADDING_CSSPX,
  topOffset     : Int = 70,
  bottomOffset  : Int = 20
) {

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

  /**
   * Посчитать кол-во колонок сетки с помощью калькулятора колонок.
   * @return Кол-во колонок сетки на экране.
   */
  def countColumns(screen: ISize2di): Int = {
    val calc = new ColumnsCountT {
      override def CELL_WIDTH_CSSPX   = cellSize
      override def TILE_PADDING_CSSPX = cellPadding
    }
    calc.getTileColsCountScr(screen)
  }

}


/** А это типа то, что сервер присылает. На самом деле там JSON (словарь). */
@js.native
sealed class MGridParamsJsonRaw extends js.Object


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
