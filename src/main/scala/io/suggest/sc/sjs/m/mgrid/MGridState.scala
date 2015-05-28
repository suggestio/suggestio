package io.suggest.sc.sjs.m.mgrid

import io.suggest.sc.sjs.m.magent.MAgent
import io.suggest.sc.tile.TileConstants

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 14:13
 * Description: Переменные состояния сетки выдачи.
 */
case class MGridState(
  var maxCellWidth      : Int     = TileConstants.CELL_WIDTH_140_CSSPX,
  var leftOffset        : Int     = 0,
  var rightOffset       : Int     = 0,
  var columnsCnt        : Int     = -1,
  var fullyLoaded       : Boolean = false,
  var adsPerLoad        : Int     = 30,
  var adsLoaded         : Int     = 0,
  var isLoadMoreRequested : Boolean = false,
  var contSz              : Option[ICwCm] = None
) {

  /** При рассчете left/right offset'ов калькулятором учитывается мнение выдачи. */
  def canNonZeroOffset: Boolean = {
    // TODO Нужно понять толком, какой смысл несет выражение в скобках...
    //cbca_grid.columns > 2 || ( cbca_grid.left_offset != 0 || cbca_grid.right_offset != 0 )
    columnsCnt > 2 || leftOffset != 0 || rightOffset != 0
  }

}


object MGridState {

  /** Предложить кол-во загружаемых за раз карточек с сервера. */
  def getAdsPerLoad(ww: Int = MAgent.availableScreen.width): Int = {
    if (ww <= 660)
      5
    else if (ww <= 800)
      10
    else if (ww <= 980)
      20
    else
      30
  }

}

