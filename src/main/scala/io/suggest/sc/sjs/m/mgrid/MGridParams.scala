package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.tile.TileConstants._

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

}
