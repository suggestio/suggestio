package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.tile.TileConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:13
 * Description: Переменные состояния сетки выдачи.
 */
case class MGridState(
  var maxCellWidth  : Int     = TileConstants.CELL_WIDTH_140_CSSPX,
  var leftOffset    : Int     = 0,
  var rightOffset   : Int     = 0,
  var columnsCnt    : Int     = -1,
  var blocks        : List[_] = Nil,
  var spacers       : List[_] = Nil
)
