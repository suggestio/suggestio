package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.tile.TileConstants._

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 13:59
 * Description: Модель настроек построения сетки. Здесь константы, которые должны выставлятьяс все одновременно.
 */

// TODO Нужно десериализацию из json прикрутить, т.к. сервер будет выставлять эти значения по своему желанию.

case class MGridParams(
  cellSize      : Int = CELL_WIDTH_140_CSSPX,
  cellPadding   : Int = PADDING_CSSPX,
  topOffset     : Int = 70,
  bottomOffset  : Int = 20
) {

  // Этот параметр лежал в MGridAds, а не тут.
  def loadModeScrollDeltaPx = 100

  /**
   * Добавить данные из json, присланного сервером.
   * @param from Исходник.
   * @return Some() и обновленные параметры grid.
   *         None, если старые параметры не изменились.
   */
  def importIfChangedFrom(from: MGridParamsJsonWrapper): Option[MGridParams] = {
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

}


/** А это типа то, что сервер присылает. На самом деле там JSON (словарь). */
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
